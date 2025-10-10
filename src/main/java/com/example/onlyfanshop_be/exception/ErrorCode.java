package com.example.onlyfanshop_be.exception;


public enum ErrorCode {
    USER_NOTEXISTED(1001, "User không tồn tại"),
    WRONGPASS(1002, "Sai mật khẩu"),
    EMAIL_USED(1004,"Email đã sử dụng"),
    USER_EXISTED(1003,"User đã tồn tại"),
    PASSWORD_NOT_MATCH(1005, "Mật khẩu và xác nhận mật khẩu không khớp"),
    PRODUCT_NOTEXISTED(1006,"Sản phẩm không tồn tại"),
    CARTITEM_NOTHING(1007,"Chưa có sản phẩm nào trong giỏ hàng"),
    CART_NOTFOUND(1008,"Không tìm thấy cart"),
    WRONG_OLD_PASSWORD(1009,"Mật khẩu cũ không đúng"),
    LOCATION_NOT_FOUND(1010,"Không thể tìm thấy địa điểm");


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
