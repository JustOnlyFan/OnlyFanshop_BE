package com.example.onlyfanshop_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "brands")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "INT UNSIGNED")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 150)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    // Legacy fields for backward compatibility
    @Transient
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Integer getBrandID() {
        return id;
    }

    @Transient
    public String getBrandName() {
        return name;
    }

    @Transient
    public String getImageURL() {
        return logoUrl;
    }

    @Transient
    public boolean isActive() {
        return true; // Always active in new schema
    }

    @OneToMany(mappedBy = "brand")
    @JsonIgnore
    private List<Product> products;
}

