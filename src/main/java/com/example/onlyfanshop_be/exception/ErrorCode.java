package com.example.onlyfanshop_be.exception;


public enum ErrorCode {
    USER_NOTEXISTED(1001, "User không tồn tại"),
    WRONGPASS(1002, "Sai mật khẩu"),
    EMAIL_USED(1004,"Email đã sử dụng"),
    USER_EXISTED(1003,"User đã tồn tại"),
    PASSWORD_NOT_MATCH(1005, "Mật khẩu và xác nhận mật khẩu không khớp");


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
