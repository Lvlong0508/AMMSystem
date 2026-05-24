CREATE TABLE IF NOT EXISTS product_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL,
    order_id VARCHAR(50) NOT NULL UNIQUE,
    quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RESERVED',
    created_at DATETIME NOT NULL DEFAULT NOW(),
    expired_at DATETIME NOT NULL,
    INDEX idx_product_status (product_id, status),
    INDEX idx_expired (status, expired_at)
);
