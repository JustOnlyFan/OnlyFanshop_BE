package com.example.onlyfanshop_be.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GHNDistrictResponse {
    @JsonProperty("DistrictID")
    private Integer districtId;
    
    @JsonProperty("ProvinceID")
    private Integer provinceId;
    
    @JsonProperty("DistrictName")
    private String districtName;
    
    @JsonProperty("Code")
    private String code;
    
    @JsonProperty("Type")
    private Integer type;
    
    @JsonProperty("SupportType")
    private Integer supportType;
    
    @JsonProperty("NameExtension")
    private List<String> nameExtension;
}
