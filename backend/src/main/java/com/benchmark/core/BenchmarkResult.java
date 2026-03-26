package com.benchmark.core;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "benchmark_result")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BenchmarkResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private String runId;

    @Column(name = "plan_name", nullable = false)
    private String planName;

    @Column(name = "db_name", nullable = false)
    private String database;

    @Column(nullable = false)
    private String workload;

    @Column(name = "record_count", nullable = false)
    private long recordCount;

    @Column(name = "batch_size", nullable = false)
    private int batchSize;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(nullable = false)
    private double throughput;

    @Column(name = "avg_latency_ms", nullable = false)
    private double avgLatencyMs;

    @Column(name = "error_count", nullable = false)
    private long errorCount;

    @Column(name = "storage_mb", nullable = false)
    private double storageMb;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
