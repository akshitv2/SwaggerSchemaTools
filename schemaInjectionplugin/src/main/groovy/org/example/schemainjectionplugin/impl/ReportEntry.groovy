package org.example.schemainjectionplugin.impl;

record ReportEntry(
        String className,
        String fieldName,
        String jsonKey,
        String matchType,
        String confidence,
        String similarity,
        String injectedRules
) {}