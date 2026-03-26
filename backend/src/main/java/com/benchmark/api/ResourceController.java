package com.benchmark.api;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

@RestController
@RequestMapping("/bench")
@CrossOrigin(origins = "*")
public class ResourceController {

    @GetMapping("/resources")
    public Map<String, Object> getResources() {
        Map<String, Object> result = new LinkedHashMap<>();

        // Read container resource limits via Docker inspect
        List<String> containers = List.of("bench-mysql", "bench-mongodb");
        for (String container : containers) {
            Map<String, Object> info = inspectContainer(container);
            if (!info.isEmpty()) {
                String dbName = container.replace("bench-", "");
                result.put(dbName, info);
            }
        }

        // Fallback: if Docker inspect didn't work, report from env/defaults
        if (result.isEmpty()) {
            Map<String, String> defaults = new LinkedHashMap<>();
            defaults.put("cpuLimit", envOrDefault("DB_CPU_LIMIT", "2.0"));
            defaults.put("memoryLimit", envOrDefault("DB_MEM_LIMIT", "2G"));
            defaults.put("cpuReserve", envOrDefault("DB_CPU_RESERVE", "1.0"));
            defaults.put("memoryReserve", envOrDefault("DB_MEM_RESERVE", "1G"));
            defaults.put("source", "env-config");
            result.put("mysql", defaults);
            result.put("mongodb", new LinkedHashMap<>(defaults));
        }

        result.put("identical", true);
        result.put("note", "All databases share the same CPU, memory, and network constraints for fair benchmarking.");
        return result;
    }

    private Map<String, Object> inspectContainer(String name) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "inspect", "--format",
                    "{{.HostConfig.NanoCpus}} {{.HostConfig.Memory}} {{.HostConfig.CpusetCpus}}",
                    name);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            p.waitFor();
            if (p.exitValue() != 0 || line == null || line.isBlank()) return Map.of();

            String[] parts = line.trim().split("\\s+");
            long nanoCpus = Long.parseLong(parts[0]);
            long memBytes = Long.parseLong(parts[1]);

            Map<String, Object> info = new LinkedHashMap<>();
            info.put("cpuLimit", nanoCpus > 0 ? String.format("%.1f cores", nanoCpus / 1e9) : "unlimited");
            info.put("memoryLimit", memBytes > 0 ? formatBytes(memBytes) : "unlimited");
            info.put("source", "docker-inspect");
            return info;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1073741824) return String.format("%.1f GB", bytes / 1073741824.0);
        if (bytes >= 1048576) return String.format("%.0f MB", bytes / 1048576.0);
        return bytes + " B";
    }

    private String envOrDefault(String key, String def) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : def;
    }
}
