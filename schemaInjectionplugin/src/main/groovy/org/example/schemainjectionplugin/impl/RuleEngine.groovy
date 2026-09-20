package org.example.schemainjectionplugin.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.text.similarity.LevenshteinDistance

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class RuleEngine {
    private final Map<String, Map<String, Object>> rules;
    private final LevenshteinDistance distance = new LevenshteinDistance();


    // Constructor for classpath InputStream fallback
    RuleEngine(InputStream jsonStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        this.rules = mapper.readValue(jsonStream, new TypeReference<Map<String, Map<String, Object>>>() {});
    }

    MatchResult findMatch(String fieldName) {
        // 1. Exact Match
        if (rules.containsKey(fieldName)) {
            return new MatchResult(fieldName, rules.get(fieldName), "Exact Match", "High", "N/A");
        }

        // 2. Normalized Match
        String normalizedTarget = normalize(fieldName);
        for (String key : rules.keySet()) {
            if (normalize(key).equalsIgnoreCase(normalizedTarget)) {
                return new MatchResult(key, rules.get(key), "Keyword/Normalized", "High", "N/A");
            }
        }

        // 3. Fuzzy Match
        String bestKey = null;
        double bestScore = 0.0;

        for (String key : rules.keySet()) {
            String normKey = normalize(key);
            double currentScore = calculateSimilarity(normalizedTarget, normKey);
            if (currentScore >= 0.75 && currentScore > bestScore) {
                bestScore = currentScore;
                bestKey = key;
            }
        }

        if (bestKey != null) {
            String simPercent = Math.round(bestScore * 100) + "%";
            return new MatchResult(bestKey, rules.get(bestKey), "Partial/Fuzzy", "Medium (Review Needed)", simPercent);
        }

        // 4. Unmatched
        return new MatchResult(null, null, "Unmatched", "Manual Action Needed", "N/A");
    }

    private static String normalize(String input) {
        String stripped = input.replaceAll("(?i)(DTO|Request|Response|Model|VO)\$", "");
        return toCamelCase(stripped);
    }

    private static String toCamelCase(String input) {
        Matcher m = Pattern.compile("([-_][a-z0-9])").matcher(input);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, m.group(1).substring(1).toUpperCase());
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private double calculateSimilarity(String s1, String s2) {
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;
        int dist = distance.apply(s1.toLowerCase(), s2.toLowerCase());
        return 1.0 - ((double) dist / maxLength);
    }
}

