-- Migration: Add inventory_request_items table
-- This allows one inventory request to contain multiple products

CREATE TABLE IF NOT EXISTS inventory_request_items (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    requested_quantity INT NOT NULL,
    approved_quantity INT NULL,
    
    INDEX idx_inv_req_item_request_id (request_id),
    INDEX idx_inv_req_item_product_id (product_id),
    
    CONSTRAINT fk_inv_req_item_request FOREIGN KEY (request_id) 
        REFERENCES inventory_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_inv_req_item_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Make product_id nullable in inventory_requests (for new multi-item requests)
ALTER TABLE inventory_requests MODIFY COLUMN product_id BIGINT UNSIGNED NULL;
ALTER TABLE inventory_requests MODIFY COLUMN requested_quantity INT NULL;

-- Migrate existing data: Create items for existing requests that have product_id
INSERT INTO inventory_request_items (request_id, product_id, requested_quantity, approved_quantity)
SELECT id, product_id, requested_quantity, approved_quantity
FROM inventory_requests
WHERE product_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM inventory_request_items WHERE request_id = inventory_requests.id
);
