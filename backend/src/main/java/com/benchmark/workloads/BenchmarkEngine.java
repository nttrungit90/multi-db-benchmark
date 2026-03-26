package com.benchmark.workloads;

import com.benchmark.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BenchmarkEngine {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkEngine.class);

    private final Map<String, DatabaseDriver> drivers;
    private final BenchmarkResultRepository resultRepository;

    public BenchmarkEngine(List<DatabaseDriver> driverList, BenchmarkResultRepository resultRepository) {
        this.drivers = driverList.stream().collect(Collectors.toMap(DatabaseDriver::name, Function.identity()));
        this.resultRepository = resultRepository;
    }

    public List<BenchmarkResult> run(BenchmarkRequest request) {
        DatabaseDriver driver = drivers.get(request.database());
        if (driver == null) {
            throw new IllegalArgumentException("Unknown database: " + request.database() +
                    ". Available: " + drivers.keySet());
        }

        String runId = UUID.randomUUID().toString().substring(0, 8);
        String planName = (request.planName() != null && !request.planName().isBlank())
                ? request.planName()
                : request.database() + "-" + runId;

        List<BenchmarkResult> results = new ArrayList<>();
        for (String workload : request.workloads()) {
            log.info("[{}] Running {} workload on {} with {} records (batch={})",
                    planName, workload, request.database(), request.recordCount(), request.batchSize());

            BenchmarkResult result = switch (workload.toLowerCase()) {
                case "insert" -> runInsert(driver, request);
                case "upsert" -> runUpsert(driver, request);
                case "read" -> runRead(driver, request);
                default -> throw new IllegalArgumentException("Unknown workload: " + workload);
            };

            result.setRunId(runId);
            result.setPlanName(planName);
            results.add(resultRepository.save(result));
            log.info("[{}] Completed {} on {}: {}ms, throughput={}/s",
                    planName, workload, request.database(), result.getDurationMs(),
                    String.format("%.0f", result.getThroughput()));
        }
        return results;
    }

    private BenchmarkResult runInsert(DatabaseDriver driver, BenchmarkRequest request) {
        driver.cleanup();
        long totalRecords = request.recordCount();
        int batchSize = request.batchSize();
        long errors = 0;

        long start = System.currentTimeMillis();
        for (long offset = 0; offset < totalRecords; offset += batchSize) {
            int count = (int) Math.min(batchSize, totalRecords - offset);
            try {
                driver.insertBatch(DataGenerator.generate(offset, count));
            } catch (Exception e) {
                errors++;
                log.warn("Insert batch error at offset {}: {}", offset, e.getMessage());
            }
        }
        long duration = System.currentTimeMillis() - start;

        return buildResult(driver, request, "insert", duration, totalRecords, errors);
    }

    private BenchmarkResult runUpsert(DatabaseDriver driver, BenchmarkRequest request) {
        long totalRecords = request.recordCount();
        int batchSize = request.batchSize();
        long errors = 0;

        long start = System.currentTimeMillis();
        for (long offset = 0; offset < totalRecords; offset += batchSize) {
            int count = (int) Math.min(batchSize, totalRecords - offset);
            try {
                driver.upsertBatch(DataGenerator.generateForUpsert(offset, count));
            } catch (Exception e) {
                errors++;
                log.warn("Upsert batch error at offset {}: {}", offset, e.getMessage());
            }
        }
        long duration = System.currentTimeMillis() - start;

        return buildResult(driver, request, "upsert", duration, totalRecords, errors);
    }

    private BenchmarkResult runRead(DatabaseDriver driver, BenchmarkRequest request) {
        long totalReads = Math.min(request.recordCount(), 100_000);
        long errors = 0;
        long totalRows = 0;

        long start = System.currentTimeMillis();
        for (long i = 1; i <= totalReads; i++) {
            try {
                List<HotelRoomPrice> rows = driver.readByHotelId(i);
                totalRows += rows.size();
            } catch (Exception e) {
                errors++;
            }
        }
        long duration = System.currentTimeMillis() - start;

        return buildResult(driver, request, "read", duration, totalReads, errors);
    }

    private BenchmarkResult buildResult(DatabaseDriver driver, BenchmarkRequest request,
                                        String workload, long durationMs, long records, long errors) {
        double throughput = durationMs > 0 ? (records * 1000.0 / durationMs) : 0;
        double avgLatency = records > 0 ? (double) durationMs / records : 0;
        double storageMb = driver.getStorageSizeBytes() / (1024.0 * 1024.0);

        return BenchmarkResult.builder()
                .database(request.database())
                .workload(workload)
                .recordCount(records)
                .batchSize(request.batchSize())
                .durationMs(durationMs)
                .throughput(throughput)
                .avgLatencyMs(avgLatency)
                .errorCount(errors)
                .storageMb(storageMb)
                .build();
    }

    public List<String> availableDatabases() {
        return new ArrayList<>(drivers.keySet());
    }

    public List<SchemaInfo> getAllSchemaInfo() {
        return drivers.values().stream()
                .map(DatabaseDriver::getSchemaInfo)
                .toList();
    }
}
