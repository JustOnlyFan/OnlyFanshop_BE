package com.example.onlyfanshop_be.exception;


public enum ErrorCode {
    USER_EXISTED(1001, "Email đã tồn tại"),

    ;


    private int code;
    private String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    public int getCode() {
        return code;
    }
    public String getMessage() {
        return message;
    }
}

