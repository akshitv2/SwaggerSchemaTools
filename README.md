# SwaggerSchemaTools

## OpenAPI Schema Injector Gradle Plugin

An automated, AST-based Java tooling engine that scans Spring Boot codebases and physically injects `@Schema(...)`
annotations into request/response DTOs based on an organization-wide JSON validation ruleset.

## Overview

Manually maintaining OpenAPI schema constraints (`@Schema(minLength = ..., pattern = ...)` across large codebases leads
to duplicate validation definitions and drift between API documentation and organizational standards.

This tool automates compliance by reading a central `rules.json` file, traversing Java source trees via Abstract Syntax
Tree (AST) parsing, and updating your `.java` files directly—complete with automatic import management and
non-destructive overrides.

---

## Key Features

* **AST-Based Source Modification:** Uses JavaParser to safely parse, modify, and rewrite Java code without relying on
  fragile regular expressions or string manipulation.
* **Idempotent / Human-Override Safe:** Skips any field that already contains an explicit `@Schema` annotation,
  preserving existing manual developer overrides.
* **Automatic Imports:** Dynamically appends `import io.swagger.v3.oas.annotations.media.Schema;` to any class where
  annotations are added.
* **Multi-Tiered Rule Resolution:** Matches field names to JSON keys using a 4-step fallback engine:

1. **Exact Match:** Direct case-sensitive lookup.
2. **Normalized Match:** Strips common DTO suffixes (`DTO`, `Request`, `Response`, `Model`, `VO`) and normalizes
   `snake_case` / `kebab-case` to `camelCase`.
3. **Fuzzy Match:** Calculates string similarity using Levenshtein distance (applies rule if similarity $\ge 75\%$).
4. **Unmatched:** Logs unmatched fields for manual developer review.


* **Audit Reporting:** Writes a detailed Markdown summary (`build/reports/validation-injection-report.md`) detailing
  match types, confidence scores, and applied constraints for code review verification.

---

## Workflow

```
[ Central Rules JSON ] 
         │
         ▼
[ Task Execution ] ──► [ AST Walk & Match Engine ] ──► [ Source Code Modified (.java) ]
                                                            │
                                                            ▼
                                                [ Audit Report (.md) ]

```

---

## How It Works

1. **Parse Rules:** Reads validation rules mapped by field/entity key from your master JSON file.
2. **Scan Source Code:** Walks `src/main/java` to parse every class declaration and field.
3. **Apply Annotations:** For fields lacking `@Schema`, the matching engine maps field names to JSON rules and injects
   relevant attributes:

* `minLength` / `maxLength`
* `pattern`
* `minimum` (`min`) / `maximum` (`max`)


4. **Generate Report:** Emits a Markdown table detailing every scanned field, match status, and confidence rating.

---

## Generated Report Preview

After running the Gradle task, a report is generated at `build/reports/validation-injection-report.md`:

| Target Class         | Target Field   | Matched JSON Key | Match Type         | Confidence Level       | Similarity | Injected Rules               |
|----------------------|----------------|------------------|--------------------|------------------------|------------|------------------------------|
| `UserProfileRequest` | `email`        | `email`          | Exact Match        | High                   | N/A        | `minLength=5, maxLength=100` |
| `UserProfileRequest` | `phone_number` | `phoneNumber`    | Keyword/Normalized | High                   | N/A        | `pattern=^\+?[1-9]\d{1,14}$` |
| `UserProfileRequest` | `usrAge`       | `age`            | Partial/Fuzzy      | Medium (Review Needed) | 78%        | `minimum=18, maximum=120`    |
| `UserProfileRequest` | `internalCode` | N/A              | Unmatched          | Manual Action Needed   | N/A        | None                         |