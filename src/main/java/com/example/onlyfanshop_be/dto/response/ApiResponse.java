package com.example.onlyfanshop_be.dto.response;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse <T> {
    private int statusCode = 200;
    private String message = "Xử lý thành công!";
    private T data;
    private Date dateTime = new Date();

    public ApiResponse(int i, String loginWithGoogleSuccess, UserDTO userDTO) {
    }
}
