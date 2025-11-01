package ru.rtc.warehouse.robot.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.rtc.warehouse.config.RobotProperties;
import ru.rtc.warehouse.robot.service.RobotDataService;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class RobotDataIntegrationTest {

    // testcontainers Postgres
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("smart_warehouse")
            .withUsername("warehouse_user")
            .withPassword("warehouse_pass");

    // testcontainers Redis (generic)
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        String redisHost = redis.getHost();
        Integer redisPort = redis.getMappedPort(6379);
        r.add("spring.redis.host", () -> redisHost);
        r.add("spring.redis.port", () -> redisPort);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RobotDataService robotDataService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RobotProperties robotProperties;

    private RedisClient lettuceClient;
    private StatefulRedisPubSubConnection<String, String> pubSubConnection;
    private BlockingQueue<String> pubsubMessages;

    @BeforeEach
    void setUp() {
        // prepare DB: insert minimal rows (warehouse, robot_status, location, product, robot)
        // Use explicit IDs to simplify assertions
        jdbcTemplate.update("INSERT INTO warehouses (id, code, zone_max_size, row_max_size, shelf_max_size, name, is_deleted) " +
                "VALUES (100, 'WH-TEST', 5, 10, 10, 'Test Warehouse', FALSE) ON CONFLICT DO NOTHING");

        jdbcTemplate.update("INSERT INTO robot_status (id, code, is_deleted) VALUES (10, 'IDLE', FALSE) ON CONFLICT DO NOTHING");

        jdbcTemplate.update("INSERT INTO location (id, warehouse_id, zone, row, shelf) VALUES (200, 100, 1, 1, 1) ON CONFLICT DO NOTHING");

        jdbcTemplate.update("INSERT INTO products (id, sku_code, name, warehouse_id, is_deleted, min_stock, optimal_stock) " +
                "VALUES (300, 'TEL-4567', 'Тестовый товар', 100, FALSE, 1, 100) ON CONFLICT DO NOTHING");

        jdbcTemplate.update("INSERT INTO robots (id, robot_code, warehouse_id, status_id, battery_level, location_id, is_deleted) " +
                "VALUES (400, 'RB-0001', 100, 10, 50, 200, FALSE) ON CONFLICT DO NOTHING");

        // ensure no leftover in recent_scans
        String recentKey = String.format(robotProperties.getRecentScansKeyTemplate(), "RB-0001");
        try {
            jdbcTemplate.getDataSource().getConnection().createStatement().execute("SELECT 1"); // noop, just to ensure connection
        } catch (Exception e) {
            // ignore
        }

        // setup Lettuce pubsub subscriber
        String redisHost = redis.getHost();
        int redisPort = redis.getMappedPort(6379);
        lettuceClient = RedisClient.create(RedisURI.create(redisHost, redisPort));
        pubSubConnection = lettuceClient.connectPubSub();
        pubsubMessages = new LinkedBlockingQueue<>();

        pubSubConnection.addListener(new RedisPubSubListener<String, String>() {
            @Override
            public void message(String channel, String message) {
                pubsubMessages.add(message);
            }
            @Override public void message(String pattern, String channel, String message) { pubsubMessages.add(message); }
            @Override public void subscribed(String channel, long count) {}
            @Override public void psubscribed(String pattern, long count) {}
            @Override public void unsubscribed(String channel, long count) {}
            @Override public void punsubscribed(String pattern, long count) {}
        });

        // subscribe to the channel the app uses
        String channel = robotProperties.getRedisChannel();
        pubSubConnection.sync().subscribe(channel);
    }

    @AfterEach
    void tearDown() {
        if (pubSubConnection != null) {
            try { pubSubConnection.close(); } catch (Exception ignored) {}
        }
        if (lettuceClient != null) {
            try { lettuceClient.shutdown(); } catch (Exception ignored) {}
        }
        // clean DB rows created (safe) - tidy up
        jdbcTemplate.update("DELETE FROM inventory_history WHERE warehouse_id = 100");
        jdbcTemplate.update("DELETE FROM robot_tokens WHERE robot_id = 400");
    }

    @Test
    void testProcessRobotData_writesInventoryAndPublishesToRedis() throws Exception {
        // Build request payload as Map -> convert to RobotDataRequest using ObjectMapper
        Map<String, Object> location = Map.of("zone", 1, "row", 1, "shelf", 1);
        Map<String, Object> scan = Map.of(
                "productCode", "TEL-4567",
                "productName", "Тестовый товар",
                "quantity", 10,
                "status", Map.of("code", "OK")
        );

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("code", "RB-0001");
        requestMap.put("timestamp", Instant.now());
        requestMap.put("location", location);
        requestMap.put("scanResults", List.of(scan));
        requestMap.put("batteryLevel", 85);
        requestMap.put("nextCheckpoint", "1-2-3");

        // convert to DTO class (must be on classpath)
        Object reqObj = objectMapper.convertValue(requestMap, Class.forName("ru.rtc.warehouse.robot.controller.dto.request.RobotDataRequest"));

        // call service (this will update DB, Redis)
        Object resp = robotDataService.getClass()
                .getMethod("processRobotData", reqObj.getClass())
                .invoke(robotDataService, reqObj);

        // assert inventory_history row created for product 300 and robot 400
        Awaitility.await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            Integer c = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM inventory_history WHERE product_id = ? AND robot_id = ?",
                    Integer.class, 300, 400);
            assertNotNull(c);
            assertTrue(c >= 1, "inventory_history row should be present");
        });

        // assert recent scans list in redis contains at least one element
        String recentKey = String.format(robotProperties.getRecentScansKeyTemplate(), "RB-0001");
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Long len = jdbcTemplate.getDataSource() == null ? 0L : 0L; // placeholder, we will use Lettuce to read list below
            // Use Lettuce connection to read the list directly
            try (var conn = lettuceClient.connect()) {
                var sync = conn.sync();
                java.util.List<String> list = sync.lrange(recentKey, -10, -1);
                assertNotNull(list);
                assertTrue(!list.isEmpty(), "recent scans list in redis should contain items");
            }
        });

        // assert a message published to channel (pubsub)
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            String m = pubsubMessages.poll();
            assertNotNull(m, "Expected pubsub message on channel");
            assertTrue(m.contains("\"type\""), "payload should be JSON with 'type'");
            assertTrue(m.contains("robot_update") || m.contains("robot_status"), "published message should be robot_update or robot_status");
        });

        // optionally check response object (RobotDataResponse) had messageId
        assertNotNull(resp);
        // response class may vary; we just ensure not null
    }
}

