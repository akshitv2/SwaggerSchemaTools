import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class OpenApiSchemaInjectorTask extends DefaultTask {

    @InputDirectory
    public abstract DirectoryProperty getSourceDir();

    @InputFile
    public abstract RegularFileProperty getRulesJson();

    @OutputFile
    public abstract RegularFileProperty getReportFile();

    @TaskAction
    public void execute() throws IOException {
        File sourceDir = getSourceDir().get().getAsFile();
        File jsonFile = getRulesJson().get().getAsFile();
        File reportFile = getReportFile().get().getAsFile();

        RuleEngine ruleEngine = new RuleEngine(jsonFile);
        CodeProcessor processor = new CodeProcessor(ruleEngine);

        List<ReportEntry> reports = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(sourceDir.toPath())) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".java"))
                 .forEach(path -> {
                     try {
                         reports.addAll(processor.processFile(path.toFile()));
                     } catch (Exception e) {
                         getLogger().error("Failed to process " + path, e);
                     }
                 });
        }

        generateMarkdownReport(reportFile, reports);
    }

    private void generateMarkdownReport(File reportFile, List<ReportEntry> reports) throws IOException {
        reportFile.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write("# OAS Validation Injection Summary Report\n\n");
            writer.write("Review this report to verify automatically injected `@Schema` annotations.\n\n");
            writer.write("| Target Class | Target Field | Matched JSON Key | Match Type | Confidence Level | Similarity | Injected Rules |\n");
            writer.write("|:---|:---|:---|:---|:---|:---|:---|\n");
            
            for (ReportEntry r : reports) {
                writer.write(String.format("| `%s` | `%s` | %s | %s | %s | %s | %s |\n",
                        r.className(), r.fieldName(),
                        r.jsonKey() != null ? "`" + r.jsonKey() + "`" : "N/A",
                        r.matchType(), r.confidence(), r.similarity(), r.injectedRules()));
            }
        }
    }
}