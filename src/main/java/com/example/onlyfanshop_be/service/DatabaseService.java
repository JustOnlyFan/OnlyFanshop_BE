package com.example.onlyfanshop_be.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for database maintenance operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Xóa toàn bộ dữ liệu trong database
     * Lưu ý: Hàm này sẽ xóa TẤT CẢ dữ liệu trong tất cả các bảng
     * Sử dụng với cẩn thận!
     */
    @Transactional
    public void deleteAllData() {
        log.warn("⚠️ Bắt đầu xóa toàn bộ dữ liệu trong database...");
        
        try {
            // Tắt foreign key checks tạm thời
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
            
            // Danh sách các bảng cần xóa (theo thứ tự để tránh lỗi foreign key)
            // Xóa từ bảng con trước, bảng cha sau
            String[] tables = {
                // Bảng con trước
                "audit_logs",
                "support_ticket_replies",
                "support_tickets",
                "blog_post_category",
                "blog_categories",
                "blog_posts",
                "pages",
                "wishlist_items",
                "wishlists",
                "product_questions",
                "product_reviews",
                "product_discounts",
                "coupon_user_usage",
                "coupons",
                "shipments",
                "payments",
                "order_items",
                "orders",
                "stock_movements",
                "warehouse_inventory",
                "warehouses",
                "cart_items",
                "carts",
                "product_images",
                "product_variants",
                "products",
                "categories",
                "brands",
                "user_addresses",
                "tokens",
                "users",
                "roles",
                "sales_channels",
                "settings"
            };
            
            int deletedCount = 0;
            for (String table : tables) {
                try {
                    int count = entityManager.createNativeQuery("TRUNCATE TABLE " + table)
                            .executeUpdate();
                    log.info("✓ Đã xóa dữ liệu từ bảng: {}", table);
                    deletedCount++;
                } catch (Exception e) {
                    // Bảng có thể không tồn tại, bỏ qua
                    log.debug("Bảng {} không tồn tại hoặc đã được xóa: {}", table, e.getMessage());
                }
            }
            
            // Bật lại foreign key checks
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
            
            log.info("✅ Hoàn thành! Đã xóa dữ liệu từ {} bảng", deletedCount);
            
        } catch (Exception e) {
            // Đảm bảo foreign key checks được bật lại ngay cả khi có lỗi
            try {
                entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
            } catch (Exception ex) {
                log.error("Lỗi khi bật lại foreign key checks: {}", ex.getMessage());
            }
            log.error("❌ Lỗi khi xóa dữ liệu: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi xóa dữ liệu database: " + e.getMessage(), e);
        }
    }

    /**
     * Xóa dữ liệu từ một bảng cụ thể
     */
    @Transactional
    public void deleteTableData(String tableName) {
        log.info("Đang xóa dữ liệu từ bảng: {}", tableName);
        try {
            entityManager.createNativeQuery("TRUNCATE TABLE " + tableName)
                    .executeUpdate();
            log.info("✓ Đã xóa dữ liệu từ bảng: {}", tableName);
        } catch (Exception e) {
            log.error("❌ Lỗi khi xóa dữ liệu từ bảng {}: {}", tableName, e.getMessage());
            throw new RuntimeException("Lỗi khi xóa dữ liệu từ bảng " + tableName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Lấy danh sách tất cả các bảng trong database
     */
    public List<String> getAllTableNames() {
        @SuppressWarnings("unchecked")
        List<String> tables = entityManager.createNativeQuery(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE()"
        ).getResultList();
        return tables;
    }
}













