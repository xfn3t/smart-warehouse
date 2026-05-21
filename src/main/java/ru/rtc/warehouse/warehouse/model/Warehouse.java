package ru.rtc.warehouse.warehouse.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.product.model.ProductWarehouse;
import ru.rtc.warehouse.user.model.User;

@Entity
@Table(name = "warehouses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(name = "zone_max_size", nullable = false)
    private Integer zoneMaxSize;

    @Column(name = "row_max_size", nullable = false)
    private Integer rowMaxSize;

    @Column(name = "shelf_max_size", nullable = false)
    private Integer shelfMaxSize;

    @Column(name = "location", length = 255)
    private String warehouseLocation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "excluded_cells", columnDefinition = "jsonb")
    private Map<String, Object> excludedCellsJson;

    @OneToMany(mappedBy = "warehouse")
    private Set<Location> locations;

    @ManyToMany
    @JoinTable(
        name = "user_warehouses",
        joinColumns = @JoinColumn(name = "warehouse_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> users = new HashSet<>();

    @OneToMany(
        mappedBy = "warehouse",
        cascade = CascadeType.ALL,
        fetch = FetchType.LAZY
    )
    private List<ProductWarehouse> productParameters = new ArrayList<>();

    @OneToMany(mappedBy = "warehouse", fetch = FetchType.LAZY)
    private List<InventoryHistory> inventoryHistory = new ArrayList<>();

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;
}
