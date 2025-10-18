package ru.rtc.warehouse.product.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

	@Id
	@Column(length = 50)
	private String id; // например TEL-XXXX

	@Column(nullable = false, length = 255)
	private String name;

	@Column(length = 100)
	private String category;

	@Column(nullable = false)
	private Integer minStock = 10;

	@Column(nullable = false)
	private Integer optimalStock = 100;

}
