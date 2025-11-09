package com.example.onlyfanshop_be.exception;


public enum ErrorCode {
    USER_NOTEXISTED(1001, "User không tồn tại"),
    WRONGPASS(1002, "Sai mật khẩu"),
    USER_EXISTED(1003,"User đã tồn tại"),
    EMAIL_USED(1004,"Email đã sử dụng"),
    USERNAME_USED(1014,"Tên người dùng đã được sử dụng"),
    PASSWORD_NOT_MATCH(1005, "Mật khẩu và xác nhận mật khẩu không khớp"),
    PRODUCT_NOTEXISTED(1006,"Sản phẩm không tồn tại"),
    CARTITEM_NOTHING(1007,"Chưa có sản phẩm nào trong giỏ hàng"),
    CART_NOTFOUND(1008,"Không tìm thấy cart"),
    WRONG_OLD_PASSWORD(1009,"Mật khẩu cũ không đúng"),
    LOCATION_NOT_FOUND(1010,"Không thể tìm thấy địa điểm"),
    NOTIFICATION_NOT_FOUND(1011,"Không tìm thấy thông báo"),
    BUY_METHOD_INVALID(1012,"Không thể xác định cách mua sản phẩm"),
    UNAUTHORIZED(1013, "Không được phép");


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
