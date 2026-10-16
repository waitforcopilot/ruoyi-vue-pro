import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.lang.model.element.Modifier;
import javax.tools.*;

/** Parse declarations with javac, without loading application classes or resolving dependencies. */
public final class SchemaFields {
    private static String annotation(ModifiersTree modifiers, String name) {
        for (AnnotationTree a : modifiers.getAnnotations()) {
            String type = a.getAnnotationType().toString();
            if (type.equals(name) || type.endsWith("." + name)) return a.toString();
        }
        return null;
    }
    private static String literal(String annotation) {
        if (annotation == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\\"([^\\\"]+)\\\"").matcher(annotation);
        return m.find() ? m.group(1) : null;
    }
    public static void main(String[] paths) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("A JDK is required for schema validation");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, files, diagnostics,
                    Arrays.asList("-proc:none", "-encoding", "UTF-8"), null, files.getJavaFileObjects(paths));
            for (CompilationUnitTree unit : task.parse()) {
                for (Tree type : unit.getTypeDecls()) {
                    if (!(type instanceof ClassTree)) continue;
                    ClassTree clazz = (ClassTree) type;
                    String table = literal(annotation(clazz.getModifiers(), "TableName"));
                    if (table == null && clazz.getSimpleName().toString().equals("BaseDO")) table = "@base";
                    if (table == null) continue;
                    String file = new File(unit.getSourceFile().toUri()).getName();
                    System.out.println(table + "\t-\t" + file);
                    if (annotation(clazz.getModifiers(), "TenantIgnore") != null) System.out.println(table + "\t@tenant-ignore\t" + file);
                    for (Tree member : clazz.getMembers()) {
                        if (!(member instanceof VariableTree)) continue;
                        VariableTree field = (VariableTree) member;
                        if (field.getModifiers().getFlags().contains(Modifier.STATIC)) continue;
                        String mapping = annotation(field.getModifiers(), "TableField");
                        if (mapping != null && mapping.matches("(?s).*\\bexist\\s*=\\s*false\\b.*")) continue;
                        String column = literal(mapping);
                        if (column == null) column = field.getName().toString().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
                        System.out.println(table + "\t" + column + "\t" + file);
                    }
                }
            }
            for (Diagnostic<?> d : diagnostics.getDiagnostics())
                if (d.getKind() == Diagnostic.Kind.ERROR) throw new IllegalStateException(d.toString());
        }
    }
}
