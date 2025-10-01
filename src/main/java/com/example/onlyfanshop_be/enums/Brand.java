package com.example.onlyfanshop_be.enums;

public enum Brand {
    // Vietnamese Fan Brands
    SENKO("Senko", "SK", "Hãng quạt phổ biến tại Việt Nam, nổi bật với độ bền, mẫu mã đa dạng và giá thành cạnh tranh"),
    ASIA("Asia", "AS", "Thương hiệu quạt điện lâu năm tại Việt Nam, được người tiêu dùng tin tưởng nhờ chất lượng ổn định"),
    
    // Japanese Brands
    PANASONIC("Panasonic", "PN", "Thương hiệu toàn cầu về điện tử và điện gia dụng, sản phẩm quạt điện bền bỉ, tiết kiệm điện"),
    MITSUBISHI("Mitsubishi", "MT", "Chuyên về thiết bị điện dân dụng và công nghiệp, quạt điện vận hành êm ái và bền bỉ"),
    TOSHIBA("Toshiba", "TS", "Đa dạng sản phẩm gia dụng, thiết kế hiện đại, quạt điện hoạt động mạnh mẽ và hiệu quả"),
    
    // Other International Brands
    DAIKIN("Daikin", "DK", "Thương hiệu Nhật Bản chuyên về điều hòa và thiết bị thông gió"),
    SHARP("Sharp", "SH", "Thương hiệu điện tử Nhật Bản với công nghệ Plasmacluster"),
    HITACHI("Hitachi", "HT", "Tập đoàn công nghệ Nhật Bản với sản phẩm quạt chất lượng cao"),
    LG("LG", "LG", "Thương hiệu Hàn Quốc với công nghệ hiện đại và thiết kế sang trọng"),
    SAMSUNG("Samsung", "SM", "Tập đoàn công nghệ Hàn Quốc với sản phẩm gia dụng thông minh"),
    
    // Chinese Brands
    XIAOMI("Xiaomi", "XM", "Thương hiệu Trung Quốc với sản phẩm công nghệ thông minh giá tốt"),
    MIDEA("Midea", "MD", "Nhà sản xuất thiết bị gia dụng hàng đầu Trung Quốc"),
    GREE("Gree", "GR", "Thương hiệu điều hòa và quạt điện nổi tiếng Trung Quốc"),
    
    // European/American Brands
    DYSON("Dyson", "DY", "Thương hiệu Anh với công nghệ quạt không cánh độc đáo"),
    HONEYWELL("Honeywell", "HW", "Thương hiệu Mỹ chuyên về thiết bị điều khiển và thông gió"),
    
    OTHER("Other", "OT", "Các thương hiệu khác không được liệt kê");

    private final String displayName;
    private final String code;
    private final String description;

    Brand(String displayName, String code, String description) {
        this.displayName = displayName;
        this.code = code;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}