package com.benchmark.api;

import com.benchmark.core.BenchmarkRequest;
import com.benchmark.core.BenchmarkResult;
import com.benchmark.core.BenchmarkResultRepository;
import com.benchmark.core.SchemaInfo;
import com.benchmark.workloads.BenchmarkEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bench")
@CrossOrigin(origins = "*")
public class BenchmarkController {

    private final BenchmarkEngine engine;
    private final BenchmarkResultRepository repository;

    public BenchmarkController(BenchmarkEngine engine, BenchmarkResultRepository repository) {
        this.engine = engine;
        this.repository = repository;
    }

    @PostMapping("/run")
    public ResponseEntity<List<BenchmarkResult>> runBenchmark(@RequestBody BenchmarkRequest request) {
        List<BenchmarkResult> results = engine.run(request);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/results")
    public ResponseEntity<List<BenchmarkResult>> getResults(
            @RequestParam(required = false) String db,
            @RequestParam(required = false) String plan) {
        List<BenchmarkResult> results;
        if (plan != null && !plan.isBlank()) {
            results = repository.findByPlanNameOrderByCreatedAtDesc(plan);
        } else if (db != null && !db.isBlank()) {
            results = repository.findByDatabaseOrderByCreatedAtDesc(db);
        } else {
            results = repository.findAllByOrderByCreatedAtDesc();
        }
        return ResponseEntity.ok(results);
    }

    @GetMapping("/databases")
    public ResponseEntity<List<String>> getDatabases() {
        return ResponseEntity.ok(engine.availableDatabases());
    }

    @GetMapping("/schemas")
    public ResponseEntity<List<SchemaInfo>> getSchemas() {
        return ResponseEntity.ok(engine.getAllSchemaInfo());
    }

    @GetMapping("/plans")
    public ResponseEntity<List<String>> getPlans() {
        return ResponseEntity.ok(repository.findDistinctPlanNames());
    }

    @DeleteMapping("/results/{id}")
    public ResponseEntity<Map<String, String>> deleteResult(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.ok(Map.of("status", "deleted", "id", id.toString()));
    }

    @DeleteMapping("/runs/{runId}")
    @Transactional
    public ResponseEntity<Map<String, String>> deleteRun(@PathVariable String runId) {
        repository.deleteByRunId(runId);
        return ResponseEntity.ok(Map.of("status", "deleted", "runId", runId));
    }

    @DeleteMapping("/results")
    public ResponseEntity<Map<String, String>> clearResults() {
        repository.deleteAll();
        return ResponseEntity.ok(Map.of("status", "cleared"));
    }
}
