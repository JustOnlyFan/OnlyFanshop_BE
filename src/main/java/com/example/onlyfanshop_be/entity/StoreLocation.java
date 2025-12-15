package com.example.onlyfanshop_be.entity;
import com.example.onlyfanshop_be.enums.StoreStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name = "store_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "INT UNSIGNED")
    @JsonProperty(value = "id", access = JsonProperty.Access.READ_ONLY)
    private Integer id;
    
    @Transient
    @JsonProperty("phoneNumber")
    public String getPhoneNumber() {
        return phone;
    }

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

    @Column(length = 150)
    private String email;

    @Column(length = 100)
    private String openingHours;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	@Builder.Default
	private StoreStatus status = StoreStatus.ACTIVE;

	@Transient
	public Boolean getIsActive() {
		return status != null && status.isOperational();
	}

	public void setIsActive(Boolean active) {
		if (active == null) {
			return;
		}
		this.status = active ? StoreStatus.ACTIVE : StoreStatus.PAUSED;
	}

	// Legacy getter for backward compatibility
	@Transient
	@JsonProperty("locationID")
	public Integer getLocationID() {
		return id;
	}
}

