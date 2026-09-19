import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class CodeProcessor {
    private final RuleEngine ruleEngine;
    private static final String SCHEMA_IMPORT = "io.swagger.v3.oas.annotations.media.Schema";

    public CodeProcessor(RuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    public List<ReportEntry> processFile(File javaFile) throws Exception {
        List<ReportEntry> reports = new ArrayList<>();
        CompilationUnit cu = StaticJavaParser.parse(javaFile);
        boolean modified = false;

        List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
        
        for (ClassOrInterfaceDeclaration clazz : classes) {
            String className = clazz.getNameAsString();
            
            for (FieldDeclaration field : clazz.getFields()) {
                if (field.getAnnotationByName("Schema").isPresent()) {
                    continue; 
                }

                for (VariableDeclarator var : field.getVariables()) {
                    String fieldName = var.getNameAsString();
                    MatchResult match = ruleEngine.findMatch(fieldName);
                    
                    if (match.rules() != null && !match.rules().isEmpty()) {
                        NormalAnnotationExpr schemaExpr = new NormalAnnotationExpr();
                        schemaExpr.setName("Schema");
                        
                        List<String> injected = new ArrayList<>();
                        
                        match.rules().forEach((k, v) -> {
                            String targetKey = normalizeRuleKey(k);
                            if (targetKey != null) {
                                if (v instanceof String) {
                                    schemaExpr.addPair(targetKey, new StringLiteralExpr(v.toString()));
                                } else if (v instanceof Number) {
                                    schemaExpr.addPair(targetKey, new IntegerLiteralExpr(v.toString()));
                                } else if (v instanceof Boolean) {
                                    schemaExpr.addPair(targetKey, new BooleanLiteralExpr((Boolean) v));
                                }
                                injected.add(targetKey + "=" + v);
                            }
                        });

                        if (!schemaExpr.getPairs().isEmpty()) {
                            field.addAnnotation(schemaExpr);
                            modified = true;
                            reports.add(new ReportEntry(className, fieldName, match.key(), match.matchType(), match.confidence(), match.similarity(), String.join(", ", injected)));
                        } else {
                            reports.add(new ReportEntry(className, fieldName, match.key(), "Unmatched", "Manual Action Needed", "N/A", "None"));
                        }
                    } else {
                        reports.add(new ReportEntry(className, fieldName, null, "Unmatched", "Manual Action Needed", "N/A", "None"));
                    }
                }
            }
        }

        if (modified) {
            cu.addImport(SCHEMA_IMPORT);
            try (FileWriter fw = new FileWriter(javaFile)) {
                fw.write(cu.toString());
            }
        }

        return reports;
    }

    private String normalizeRuleKey(String key) {
        return switch (key) {
            case "min" -> "minimum";
            case "max" -> "maximum";
            case "minLength", "maxLength", "pattern", "minimum", "maximum" -> key;
            default -> null;
        };
    }
}