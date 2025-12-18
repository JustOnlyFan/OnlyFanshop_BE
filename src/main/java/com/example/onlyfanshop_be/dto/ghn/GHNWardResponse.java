package com.example.onlyfanshop_be.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GHNWardResponse {
    @JsonProperty("WardCode")
    private String wardCode;
    
    @JsonProperty("DistrictID")
    private Integer districtId;
    
    @JsonProperty("WardName")
    private String wardName;
    
    @JsonProperty("NameExtension")
    private List<String> nameExtension;
}
