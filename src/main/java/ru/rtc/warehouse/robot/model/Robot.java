package ru.rtc.warehouse.robot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import ru.rtc.warehouse.warehouse.model.Warehouse;

@Entity
@Table(name = "robots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Robot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "robot_code", length = 50, nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(optional = false, cascade = CascadeType.MERGE)
    @JoinColumn(name = "status_id", nullable = false)
    private RobotStatus status;

    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Column(name = "last_update")
    private LocalDateTime lastUpdate;

    @Column(name = "current_zone")
    private Integer currentZone;

    @Column(name = "current_row")
    private Integer currentRow;

    @Column(name = "current_shelf")
    private Integer currentShelf;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;
}
