package com.benchmark.core;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BenchmarkResultRepository extends JpaRepository<BenchmarkResult, Long> {
    List<BenchmarkResult> findByDatabaseOrderByCreatedAtDesc(String database);
    List<BenchmarkResult> findAllByOrderByCreatedAtDesc();
    List<BenchmarkResult> findByRunIdOrderByCreatedAtAsc(String runId);
    List<BenchmarkResult> findByPlanNameOrderByCreatedAtDesc(String planName);
    void deleteByRunId(String runId);

    @Query("SELECT DISTINCT r.planName FROM BenchmarkResult r ORDER BY r.planName")
    List<String> findDistinctPlanNames();

    @Query("SELECT DISTINCT r.runId FROM BenchmarkResult r ORDER BY r.runId DESC")
    List<String> findDistinctRunIds();
}
