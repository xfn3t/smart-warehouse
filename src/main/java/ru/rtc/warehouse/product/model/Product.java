package ru.rtc.warehouse.product.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.user.model.User;

@Entity
@Table(
    name = "products",
    uniqueConstraints = @UniqueConstraint(
        columnNames = { "user_id", "sku_code" }
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "sku_code", nullable = false)
    private String skuCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category")
    private String category;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @OneToMany(
        mappedBy = "product",
        cascade = CascadeType.ALL,
        fetch = FetchType.LAZY
    )
    private List<ProductWarehouse> warehouseParameters = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<InventoryHistory> inventoryHistory = new ArrayList<>();
}
