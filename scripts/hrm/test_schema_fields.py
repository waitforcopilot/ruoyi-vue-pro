"""Regression for real Java declarations that the old regular expression silently missed."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


class SchemaFieldsTest(unittest.TestCase):
    def test_declarators_annotations_initializers_and_nested_classes(self):
        with tempfile.TemporaryDirectory(prefix="hrm-schema-test-") as directory:
            compiler = shutil.which("javac")
            self.assertIsNotNone(compiler, "A JDK is required")
            tools = Path(compiler).resolve().parents[1] / "lib/tools.jar"
            cp = os.pathsep.join([directory, str(tools)]) if tools.exists() else directory
            fixture = Path(directory) / "Example.java"
            fixture.write_text('''
@TenantIgnore
@TableName(value = "test_table")
class Example extends BaseDO {
 private Long firstId, secondId;
 private java.util.Map<String, java.util.List<Long>> first, second;
 @TableField(exist = false) private String ignored, alsoIgnored;
 @TableField(value = "explicit_column", fill = FieldFill.INSERT) private String mapped;
 private String json = "{private Long fake;}", next = "a,b";
 private int[] indexes = {1, 2}, others;
 private static final String NO_COLUMN = "ignore";
 // private String commented;
 class Nested {private Long nestedId;}
 private Object value = new Object() {private String anonymous;};
}
class BaseDO {private Long inherited;}
''', encoding="utf-8")
            helper = Path(__file__).with_name("SchemaFields.java")
            subprocess.run([compiler, "-encoding", "UTF-8", "-cp", cp, "-d", directory, str(helper)], check=True)
            lines = subprocess.check_output(["java", "-cp", cp, "SchemaFields", str(fixture)], text=True).splitlines()
            self.assertEqual({line.split("\t")[1] for line in lines if line.startswith("test_table\t")},
                             {"-", "@tenant-ignore", "first_id", "second_id", "first", "second", "explicit_column", "json", "next", "indexes", "others", "value"})
            self.assertIn("@base\tinherited\tExample.java", lines)


if __name__ == "__main__":
    unittest.main()
