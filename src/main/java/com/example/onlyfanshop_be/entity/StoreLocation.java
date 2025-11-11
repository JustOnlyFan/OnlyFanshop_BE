package com.example.onlyfanshop_be.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name = "StoreLocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer locationID;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false, length = 255)
    private String address;

	// Optional granular address parts
	@Column(length = 100)
	private String ward;

	@Column(length = 100)
	private String city;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String openingHours;

	@Column(name = "is_active", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
	@Builder.Default
	private Boolean isActive = true;

    @OneToOne(mappedBy = "storeLocation", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Warehouse warehouse;
}

