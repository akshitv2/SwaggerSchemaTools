package org.example.schemainjectionplugin;

import org.example.schemainjectionplugin.impl.OpenApiSchemaInjectorTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

class OpenApiSchemaInjectorPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getTasks().register("injectOpenApiSchema", OpenApiSchemaInjectorTask.class, task -> {
            task.setGroup("documentation");
            task.setDescription("Scans DTOs and injects Swagger @Schema annotations using rules.json.");

            task.getSourceDir().convention(project.getLayout().getProjectDirectory().dir("src/main/java"));
//            task.getRulesJson().convention(getClass().getResource("/rules.json"));
            task.getReportFile().convention(project.getLayout().getBuildDirectory().file("reports/validation-injection-report.md"));
        });
    }
}