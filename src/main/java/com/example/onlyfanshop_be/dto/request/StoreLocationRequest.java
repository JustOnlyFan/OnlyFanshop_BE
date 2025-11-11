package com.example.onlyfanshop_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreLocationRequest {
	@NotBlank(message = "name is required")
	@Size(max = 255, message = "name must be <= 255 characters")
	private String name;

	@Size(max = 500, message = "description must be <= 500 characters")
	private String description;

	@Size(max = 500, message = "imageUrl must be <= 500 characters")
	private String imageUrl;

	@NotNull(message = "latitude is required")
	private Double latitude;

	@NotNull(message = "longitude is required")
	private Double longitude;

	@NotBlank(message = "address is required")
	@Size(max = 255, message = "address must be <= 255 characters")
	private String address;

	@Size(max = 20, message = "phone must be <= 20 characters")
	private String phone;

	@Size(max = 100, message = "openingHours must be <= 100 characters")
	private String openingHours;

	// Optional extra fields from frontend
	@Size(max = 100, message = "ward must be <= 100 characters")
	private String ward;

	@Size(max = 100, message = "city must be <= 100 characters")
	private String city;

	// Frontend may send phoneNumber instead of phone
	@Size(max = 20, message = "phoneNumber must be <= 20 characters")
	private String phoneNumber;

	// Frontend may send multiple images; we take the first as imageUrl
	private java.util.List<String> images;

	// If provided, create a BRANCH warehouse for this store under a REGIONAL warehouse
	private Integer parentRegionalWarehouseId;
}


