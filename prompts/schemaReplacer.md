# Role & Task Objective

You are an expert Java, Gradle, and Spring Boot developer.

Your task is to build a standalone, reusable **Gradle Custom Task / Java Tooling Engine** that scans Java controllers,
request/response DTOs, and nested POJOs in a Spring Boot codebase. It reads a master JSON file containing
organization-wide validation rules (key-value pairs mapped to object and field names) and physically modifies the
`.java` source code files by injecting OpenAPI `@Schema(...)` annotations (
`io.swagger.v3.oas.annotations.media.Schema`).

The tool must use AST (Abstract Syntax Tree) parsing rather than regex or string manipulation. After modifying the
source code, it must output a Markdown summary report detailing match confidences so developers can review changes in
Git.

---

## Technical Stack Requirements

- **Language:** Java 17+
- **Build System:** Gradle (as a custom build plugin or executable task)
- **AST Parsing Engine:** JavaParser (`com.github.javaparser:javaparser-core`) OR OpenRewrite
- **Fuzzy Matching/String Distance:** Apache Commons Text (`org.apache.commons:commons-text`) for `LevenshteinDistance`
- **JSON Engine:** Jackson (`com.fasterxml.jackson.core:jackson-databind`)

---

## Core Feature Requirements

### 1. AST Source Code Transformation

- Walk through `src/main/java` and discover all POJO classes (Request bodies, Response payloads, nested DTOs,
  Enums/Objects).
- Inspect field declarations inside each class.
- **Idempotency Rule:** Skip any field that already has an explicit `@Schema` annotation manually attached to it,
  preserving human overrides.
- Automatically add the required import `import io.swagger.v3.oas.annotations.media.Schema;` if `@Schema` is applied to
  any field in the class.
- Inject `@Schema(...)` parameters based on supported validation keys found in the JSON:
    - `minLength` -> `@Schema(minLength = X)`
    - `maxLength` -> `@Schema(maxLength = X)`
    - `pattern` -> `@Schema(pattern = "...")`
    - `min` / `minimum` -> `@Schema(minimum = "X")`
    - `max` / `maximum` -> `@Schema(maximum = "X")`

---

### 2. Multi-Tiered Matching Engine (JSON Key Resolution)

When evaluating a Java Class Name or Field Name against keys in the central JSON validations dictionary, use a fallback
chain:

1. **Exact Match (Score: 1.0):** Case-sensitive or exact string match against the JSON key.
2. **Normalized / Keyword Match (Score: 1.0):**
    - Strip suffixes like `DTO`, `Request`, `Response`, `Model`, `VO` from class names.
    - Convert `snake_case` or `kebab-case` to `camelCase`.
    - Match ignoring case sensitivity.
3. **Fuzzy / Partial Match (Score: variable >= 0.75):**
    - Use Levenshtein Distance or Jaro-Winkler distance on normalized strings.
    - If similarity score is $\ge 0.75$ (75%), select the highest-scoring candidate key.
4. **Unmatched (Score: 0.0):** No match found in the JSON dictionary above 0.75 threshold.

---

### 3. Reporting Engine (`validation-report.md`)

The tool must write an in-memory audit log during execution and generate a Markdown file at
`build/reports/validation-injection-report.md` once complete.

#### Markdown Output Format:
```markdown

# OAS Validation Injection Summary Report

Review this report to verify automatically injected `@Schema` annotations.

| Target Class         | Target Field   | Matched JSON Key | Match Type         | Confidence Level       | Similarity | Injected Rules               |
|:---------------------|:---------------|:-----------------|:-------------------|:-----------------------|:-----------|:-----------------------------|
| `UserProfileRequest` | `email`        | `email`          | Exact Match        | High                   | N/A        | `minLength=5, maxLength=100` |
| `UserProfileRequest` | `phone_number` | `phoneNumber`    | Keyword/Normalized | High                   | N/A        | `pattern=^\+?[1-9]\d{1,14}$` |
| `UserProfileRequest` | `usrAge`       | `age`            | Partial/Fuzzy      | Medium (Review Needed) | 78%        | `minimum=18, maximum=120`    |
| `UserProfileRequest` | `internalCode` | N/A              | Unmatched          | Manual Action Needed   | N/A        | None                         |

```