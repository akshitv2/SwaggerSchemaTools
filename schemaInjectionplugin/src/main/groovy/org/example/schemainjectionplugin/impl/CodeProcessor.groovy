package org.example.schemainjectionplugin.impl

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*

import java.lang.reflect.Array

class CodeProcessor {
    private final RuleEngine ruleEngine;
    private static final String SCHEMA_IMPORT = "io.swagger.v3.oas.annotations.media.Schema";

    CodeProcessor(RuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    List<ReportEntry> processFile(File javaFile) throws Exception {
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
                            if (k != null && !k.isBlank() && v != null) {
                                Expression valueExpr = toExpression(v);
                                if (valueExpr != null) {
                                    schemaExpr.addPair(k, valueExpr);
                                    injected.add(k + "=" + v);
                                }
                            }
                        })

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

    private Expression toExpression(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof String) {
            return new StringLiteralExpr((String) v);
        } else if (v instanceof Boolean) {
            return new BooleanLiteralExpr((Boolean) v);
        } else if (v instanceof Integer || v instanceof Long || v instanceof Short || v instanceof Byte) {
            return new IntegerLiteralExpr(v.toString());
        } else if (v instanceof Double || v instanceof Float) {
            return new DoubleLiteralExpr(v.toString());
        } else if (v instanceof Collection<?>) {
            NodeList<Expression> expressions = new NodeList<>();
            for (Object item : (Collection<?>) v) {
                Expression expr = toExpression(item);
                if (expr != null) {
                    expressions.add(expr);
                }
            }
            return new ArrayInitializerExpr(expressions);
        } else if (v.getClass().isArray()) {
            NodeList<Expression> expressions = new NodeList<>();
            int length = Array.getLength(v);
            for (int i = 0; i < length; i++) {
                Object item = Array.get(v, i);
                Expression expr = toExpression(item);
                if (expr != null) {
                    expressions.add(expr);
                }
            }
            return new ArrayInitializerExpr(expressions);
        }
        return new StringLiteralExpr(v.toString());
    }
}