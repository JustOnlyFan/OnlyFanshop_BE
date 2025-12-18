package com.example.onlyfanshop_be.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GHNResponse<T> {
    private Integer code;
    private String message;
    private T data;
    
    @JsonProperty("code_message_value")
    private String codeMessageValue;
    
    public boolean isSuccess() {
        return code != null && code == 200;
    }
}
