CREATE TABLE debt_items
(
    id                 BIGINT UNSIGNED AUTO_INCREMENT NOT NULL,
    debt_order_id      BIGINT UNSIGNED                NOT NULL,
    product_id         BIGINT UNSIGNED                NOT NULL,
    owed_quantity      INT                            NOT NULL,
    fulfilled_quantity INT DEFAULT 0                  NOT NULL,
    CONSTRAINT pk_debt_items PRIMARY KEY (id)
);

ALTER TABLE debt_items
    ADD CONSTRAINT FK_DEBT_ITEMS_ON_DEBT_ORDER FOREIGN KEY (debt_order_id) REFERENCES debt_orders (id);

CREATE INDEX idx_debt_item_debt_order_id ON debt_items (debt_order_id);

ALTER TABLE debt_items
    ADD CONSTRAINT FK_DEBT_ITEMS_ON_PRODUCT FOREIGN KEY (product_id) REFERENCES products (id);

CREATE INDEX idx_debt_item_product_id ON debt_items (product_id);