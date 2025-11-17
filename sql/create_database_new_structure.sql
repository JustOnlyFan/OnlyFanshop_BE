-- Script SQL để tạo lại database với cấu trúc mới
-- Bỏ toàn bộ warehouse, chỉ còn StoreInventory

-- Drop các bảng cũ liên quan đến warehouse (nếu có)
DROP TABLE IF EXISTS stock_movements;
DROP TABLE IF EXISTS warehouse_inventory;
DROP TABLE IF EXISTS warehouses;

-- Bảng StoreLocations (đã có sẵn, không cần tạo lại)
-- Chỉ cần đảm bảo có đầy đủ các cột cần thiết

-- Bảng StoreInventory (mỗi store có kho riêng, quản lý sản phẩm)
CREATE TABLE IF NOT EXISTS store_inventory (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    store_id INT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    is_available TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'true = store có bán sản phẩm này, false = không bán (admin bật/tắt)',
    quantity INT DEFAULT 0 COMMENT 'Số lượng tồn kho tại store này',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Constraints
    UNIQUE KEY uniq_store_product (store_id, product_id),
    INDEX idx_store_inventory_store_id (store_id),
    INDEX idx_store_inventory_product_id (product_id),
    INDEX idx_store_inventory_is_available (is_available),
    
    -- Foreign keys
    CONSTRAINT fk_store_inventory_store FOREIGN KEY (store_id) 
        REFERENCES StoreLocations(locationID) ON DELETE CASCADE,
    CONSTRAINT fk_store_inventory_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Ghi chú:
-- 1. Mỗi store (StoreLocation) có kho riêng (store_inventory)
-- 2. Khi tạo sản phẩm mới, tự động thêm vào tất cả stores (is_available = true)
-- 3. Admin có thể bật/tắt việc bán sản phẩm ở mỗi store (is_available)
-- 4. quantity: số lượng tồn kho tại store (optional, có thể để null nếu không theo dõi số lượng)


