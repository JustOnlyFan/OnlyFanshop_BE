package com.example.onlyfanshop_be.exception;


import com.example.onlyfanshop_be.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<?>> handleAppException(AppException e) {
        ErrorCode errorCode = e.getErrorCode();
        ApiResponse<?> response = ApiResponse.builder()
                .statusCode(errorCode.getCode())
                .message(e.getMessage())
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    // Handle AccessDeniedException BEFORE RuntimeException (order matters!)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDeniedException(AccessDeniedException e) {
        ApiResponse<?> response = ApiResponse.builder()
                .statusCode(403)
                .message("Access Denied: " + (e.getMessage() != null ? e.getMessage() : "You do not have permission to access this resource"))
                .build();
        System.err.println("Access Denied: " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException e) {
        StringBuilder errorMessage = new StringBuilder("Validation failed: ");
        e.getBindingResult().getFieldErrors().forEach(error -> {
            errorMessage.append(error.getField()).append(" - ").append(error.getDefaultMessage()).append("; ");
        });
        ApiResponse<?> response = ApiResponse.builder()
                .statusCode(400)
                .message(errorMessage.toString())
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<?>> handleRuntimeException(RuntimeException e) {
        // Don't handle AccessDeniedException here - it's handled above
        if (e instanceof AccessDeniedException) {
            return handleAccessDeniedException((AccessDeniedException) e);
        }
        
        ApiResponse<?> response = ApiResponse.builder()
                .statusCode(400)
                .message(e.getMessage() != null ? e.getMessage() : "Runtime error occurred")
                .build();
        System.err.println("RuntimeException: " + e.getClass().getName() + " - " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneralException(Exception e) {
        // Don't handle specific exceptions here - they're handled above
        if (e instanceof AccessDeniedException) {
            return handleAccessDeniedException((AccessDeniedException) e);
        }
        if (e instanceof RuntimeException) {
            return handleRuntimeException((RuntimeException) e);
        }
        
        ApiResponse<?> response = ApiResponse.builder()
                .statusCode(500)
                .message(e.getMessage() != null ? e.getMessage() : "An unexpected error occurred")
                .build();
        System.err.println("Unexpected exception: " + e.getClass().getName() + " - " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}


