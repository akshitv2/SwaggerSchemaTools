public record ReportEntry(
        String className,
        String fieldName,
        String jsonKey,
        String matchType,
        String confidence,
        String similarity,
        String injectedRules
) {}