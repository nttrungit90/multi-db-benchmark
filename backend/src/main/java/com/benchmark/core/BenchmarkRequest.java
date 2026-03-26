package com.benchmark.core;

import java.util.List;

public record BenchmarkRequest(
        String database,
        List<String> workloads,
        long recordCount,
        int batchSize,
        String planName
) {}
