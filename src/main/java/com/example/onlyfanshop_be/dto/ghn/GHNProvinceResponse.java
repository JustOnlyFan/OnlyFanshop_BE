package com.example.onlyfanshop_be.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GHNProvinceResponse {
    @JsonProperty("ProvinceID")
    private Integer provinceId;
    
    @JsonProperty("ProvinceName")
    private String provinceName;
    
    @JsonProperty("CountryID")
    private Integer countryId;
    
    @JsonProperty("Code")
    private String code;
    
    @JsonProperty("NameExtension")
    private List<String> nameExtension;
}
