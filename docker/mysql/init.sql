CREATE TABLE IF NOT EXISTS hotel_room_latest_price (
    hotel_id BIGINT NOT NULL,
    wego_room_type_id BIGINT NOT NULL,
    supplier_code VARCHAR(50) NOT NULL,
    supplier_channel VARCHAR(50) NOT NULL,
    checkin_date DATE NOT NULL,
    final_price DECIMAL(12, 2) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (hotel_id, wego_room_type_id, supplier_code, supplier_channel, checkin_date)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS benchmark_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id VARCHAR(50) NOT NULL,
    plan_name VARCHAR(200) NOT NULL,
    db_name VARCHAR(50) NOT NULL,
    workload VARCHAR(50) NOT NULL,
    record_count BIGINT NOT NULL,
    batch_size INT NOT NULL,
    duration_ms BIGINT NOT NULL,
    throughput DOUBLE NOT NULL,
    avg_latency_ms DOUBLE NOT NULL,
    error_count BIGINT NOT NULL DEFAULT 0,
    storage_mb DOUBLE NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_run_id (run_id),
    INDEX idx_plan_name (plan_name)
);
