package com.example.onlyfanshop_be.enums;

/**
 * Enum representing different fan product categories available in the system
 */
public enum Category {
    // Main Fan Categories
    QUAT_DIEN_2_TRONG_1("Quạt điện 2 trong 1", "Q2T1", "Quạt kết hợp nhiều chức năng, có thể sử dụng làm quạt đứng và quạt bàn"),
    QUAT_DUNG("Quạt đứng", "QD", "Quạt có chân đế cao, có thể điều chỉnh độ cao và hướng gió"),
    QUAT_TREO_TUONG("Quạt treo tường", "QTT", "Quạt gắn cố định trên tường, tiết kiệm không gian"),
    QUAT_TUAN_HOAN("Quạt tuần hoàn", "QTH", "Quạt tạo luồng không khí tuần hoàn trong phòng"),
    QUAT_TRAN("Quạt trần", "QT", "Quạt gắn trên trần nhà, phù hợp cho không gian rộng"),
    QUAT_THONG_GIO("Quạt thông gió", "QTG", "Quạt chuyên dụng để thông gió, trao đổi không khí"),
    QUAT_HUT("Quạt hút", "QH", "Quạt hút khí độc, mùi hôi ra ngoài"),
    QUAT_BAN("Quạt bàn", "QB", "Quạt nhỏ gọn đặt trên bàn làm việc"),
    QUAT_LUNG("Quạt lửng", "QL", "Quạt có độ cao trung bình, thường dùng trong gia đình"),
    QUAT_HOP("Quạt hộp", "QHP", "Quạt hình hộp vuông, thường dùng để thông gió"),
    QUAT_DAO_TRAN("Quạt đảo trần", "QDT", "Quạt trần có thể đảo chiều quay"),
    QUAT_DUNG_CONG_NGHIEP("Quạt đứng công nghiệp", "QDCN", "Quạt công suất lớn dùng trong nhà xưởng, kho bãi"),
    
    // Advanced Fan Types
    QUAT_DIEU_HOA("Quạt điều hòa", "QDH", "Quạt kết hợp làm mát bằng nước, tiết kiệm điện"),
    QUAT_KHONG_CANH("Quạt không cánh", "QKC", "Quạt sử dụng công nghệ hiện đại không có cánh quạt truyền thống"),
    QUAT_ION("Quạt ion", "QI", "Quạt tích hợp công nghệ ion âm, lọc không khí"),
    QUAT_SIEU_AM("Quạt siêu âm", "QSA", "Quạt tạo độ ẩm bằng công nghệ siêu âm"),
    QUAT_PHUN_SUONG("Quạt phun sương", "QPS", "Quạt tích hợp hệ thống phun sương làm mát"),
    
    // Specialized Fans
    QUAT_HUT_MUI("Quạt hút mùi", "QHM", "Quạt chuyên dụng cho nhà bếp, hút mùi thức ăn"),
    QUAT_THONG_GIO_NHA_TAM("Quạt thông gió nhà tắm", "QTGNT", "Quạt chuyên dụng cho nhà tắm, chống ẩm"),
    QUAT_CONG_NGHIEP("Quạt công nghiệp", "QCN", "Quạt công suất lớn dùng trong sản xuất"),
    QUAT_NONG_NGHIEP("Quạt nông nghiệp", "QNN", "Quạt chuyên dụng cho nông nghiệp, chăn nuôi"),
    
    // Portable Fans
    QUAT_MINI("Quạt mini", "QM", "Quạt nhỏ gọn, dễ mang theo"),
    QUAT_USB("Quạt USB", "QUSB", "Quạt nhỏ cắm qua cổng USB"),
    QUAT_PIN("Quạt pin", "QP", "Quạt sạc pin, không cần dây điện"),
    QUAT_CAM_TAY("Quạt cầm tay", "QCT", "Quạt nhỏ cầm tay, tiện lợi"),
    
    // Accessories and Parts
    PHU_KIEN_QUAT("Phụ kiện quạt", "PKQ", "Các phụ kiện thay thế cho quạt"),
    CANH_QUAT("Cánh quạt", "CQ", "Cánh quạt thay thế"),
    DONG_CO_QUAT("Động cơ quạt", "DCQ", "Động cơ thay thế cho quạt"),
    DIEU_KHIEN_QUAT("Điều khiển quạt", "DKQ", "Remote điều khiển từ xa"),
    
    OTHER("Khác", "OTH", "Các loại quạt khác không được phân loại");

    private final String displayName;
    private final String code;
    private final String description;

    Category(String displayName, String code, String description) {
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