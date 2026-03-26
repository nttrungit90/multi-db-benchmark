package com.benchmark.core;

import java.util.List;

public record SchemaInfo(
        String database,
        String schemaDefinition,
        List<String> indexes,
        String insertCommand,
        String upsertCommand,
        String readCommand,
        String primaryKey
) {}
