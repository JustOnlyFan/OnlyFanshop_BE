package com.example.onlyfanshop_be.exception;


import com.example.onlyfanshop_be.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse> handleRuntimeException(Exception e) {
        ApiResponse apiReponse = new ApiResponse();

        apiReponse.setStatusCode(9999);
        apiReponse.setMessage(e.getMessage());

        return ResponseEntity.badRequest().body(apiReponse);
    }

    @ExceptionHandler(value = RuntimeException.class)
    ResponseEntity<ApiResponse> handleAppException(AppException e) {
        ErrorCode errorCode = e.getErrorCode();
        ApiResponse apiReponse = new ApiResponse();

        apiReponse.setStatusCode(errorCode.getCode());
        apiReponse.setMessage(e.getMessage());

        return ResponseEntity.badRequest().body(apiReponse);
    }
}
