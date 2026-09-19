package org.example.schemainjectionplugin.impl;

import java.util.Map;

record MatchResult(String key, Map<String, Object> rules, String matchType, String confidence, String similarity) {}