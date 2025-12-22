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
    UNAUTHORIZED(1013, "Không được phép"),
	INVALID_INPUT(1014, "Dữ liệu không hợp lệ"),
    WRONG_PORTAL_ADMIN(1021, "Bạn đang dùng sai trang web. Vui lòng sử dụng trang Admin để đăng nhập."),
    WRONG_PORTAL_STAFF(1022, "Bạn đang dùng sai trang web. Vui lòng sử dụng trang Staff để đăng nhập."),
    WRONG_PORTAL_CUSTOMER(1023, "Bạn đang dùng sai trang web. Vui lòng sử dụng trang khách hàng để đăng nhập."),
    WAREHOUSE_NOT_FOUND(1015, "Không tìm thấy kho hàng"),
    WAREHOUSE_CODE_EXISTS(1016, "Mã kho hàng đã tồn tại"),
    WAREHOUSE_INVENTORY_NOT_FOUND(1017, "Không tìm thấy tồn kho"),
    INSUFFICIENT_STOCK(1018, "Không đủ hàng trong kho"),
    INVALID_WAREHOUSE_TRANSFER(1019, "Không thể chuyển kho"),
	INVALID_WAREHOUSE_TYPE(1020, "Loại kho hàng không hợp lệ"),
    
    // Category error codes
    CATEGORY_NOT_FOUND(2001, "Không tìm thấy danh mục"),
    CATEGORY_TYPE_REQUIRED(2002, "Loại danh mục là bắt buộc"),
    PARENT_NOT_FOUND(2003, "Danh mục cha không tồn tại"),
    CATEGORY_TYPE_MISMATCH(2004, "Danh mục con phải cùng loại với danh mục cha"),
    CATEGORY_HAS_CHILDREN(2005, "Không thể xóa danh mục có danh mục con"),
    CATEGORY_MAX_DEPTH_EXCEEDED(2006, "Danh mục đã đạt độ sâu tối đa (3 cấp)"),
    CATEGORY_NAME_EXISTS(2007, "Tên danh mục đã tồn tại"),
    DUPLICATE_TAG(2008, "Mã tag đã tồn tại"),
    REQUIRED_CATEGORY_MISSING(2009, "Sản phẩm phải có danh mục FAN_TYPE hoặc ACCESSORY_TYPE"),
    INVALID_PRICE_RANGE(2010, "Giá tối thiểu phải nhỏ hơn giá tối đa"),
    
    // Transfer Request error codes
    TRANSFER_REQUEST_NOT_FOUND(3001, "Không tìm thấy yêu cầu chuyển kho"),
    TRANSFER_REQUEST_ALREADY_PROCESSED(3002, "Yêu cầu chuyển kho đã được xử lý"),
    TRANSFER_REQUEST_INVALID_STATUS(3003, "Trạng thái yêu cầu không hợp lệ"),
    TRANSFER_REQUEST_QUANTITY_EXCEEDS_LIMIT(3004, "Số lượng yêu cầu vượt quá giới hạn (tối đa 30 sản phẩm mỗi loại)"),
    TRANSFER_REQUEST_PRODUCT_NOT_IN_STORE(3005, "Sản phẩm không tồn tại trong kho cửa hàng"),
    TRANSFER_REQUEST_EMPTY_ITEMS(3006, "Yêu cầu chuyển kho phải có ít nhất một sản phẩm"),
    TRANSFER_REQUEST_INVALID_QUANTITY(3007, "Số lượng yêu cầu phải lớn hơn 0"),
    STORE_NOT_FOUND(3008, "Không tìm thấy cửa hàng"),
    
    // Debt Order error codes
    DEBT_ORDER_NOT_FOUND(4001, "Không tìm thấy đơn nợ"),
    DEBT_ORDER_ALREADY_COMPLETED(4002, "Đơn nợ đã được hoàn thành"),
    DEBT_ORDER_CANNOT_FULFILL(4003, "Không đủ hàng để đáp ứng đơn nợ"),
    DEBT_ORDER_EMPTY_ITEMS(4004, "Đơn nợ phải có ít nhất một sản phẩm"),
    DEBT_ORDER_ALREADY_EXISTS(4005, "Đơn nợ đã tồn tại cho yêu cầu chuyển kho này"),
    
	UNCATEGORIZED_EXCEPTION(1099, "...");


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
