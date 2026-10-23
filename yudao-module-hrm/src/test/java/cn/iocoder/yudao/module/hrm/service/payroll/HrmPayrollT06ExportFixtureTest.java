package cn.iocoder.yudao.module.hrm.service.payroll;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Synthetic projection -> real production exporter -> POI readback; no DB or browser. */
class HrmPayrollT06ExportFixtureTest {
    private static final String OUTPUT_PROPERTY = "hrm.payroll.t06.fixtureDir";
    private static final String ORIGIN = "TEST 合成投影，经真实生产 workbook() 导出；非浏览器下载、非DB快照、非正式批准";

    @Test
    void realExporterPreservesBoundaryTextAndOptionallySavesNativeReviewFixtures() throws Exception {
        Map<String, List<Map<String, Object>>> longTables = emptyTables();
        StringBuilder body = new StringBuilder("TEST 长正文开始\n");
        for (int n = 0; n < 7000; n++) body.append("中文长正文🌿\n\n\n\n\n\n");
        body.append("TEST 长正文结束🌿");
        StringBuilder basis = new StringBuilder("TEST 超长依据开始：");
        for (int n = 0; n < 2000; n++) basis.append("第").append(n).append("条：仅为合成验收依据🌿，不代表实际授权或正式业务批准。");
        basis.append("TEST 超长依据结束🌿");
        for (int n = 0; n < HrmPayrollGovernanceProjectionService.SHEETS.size(); n++) {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("记录类型", "TEST_FIXTURE");
            record.put("记录号", "TEST-T06-" + (n + 1));
            record.put("夹具来源", ORIGIN);
            record.put("正文", n == 0 ? body.toString() : "TEST 六表原生阅读样例：中文、emoji🌿与段落。\n\n第二段结束🌿");
            record.put("授权与发布依据", n == 5 ? basis.toString() : "TEST 合成依据，不代表实际授权");
            record.put("连续换行", "TEST 首行🌿\n\n\n\n\n\n\n\n\n\n\n\n末行🌿");
            record.put("公式样例", "=1+1");
            record.put("空字符串", "");
            record.put("空值", null);
            record.put("合成结构", Collections.singletonMap("说明", "TEST JSON中文🌿\n换行"));
            longTables.get(HrmPayrollGovernanceProjectionService.SHEETS.get(n)).add(record);
        }
        Map<String, List<Map<String, Object>>> emptyTables = emptyTables();
        byte[] longBytes = exportAndReadBack(longTables);
        byte[] emptyBytes = exportAndReadBack(emptyTables);
        String output = System.getProperty(OUTPUT_PROPERTY);
        if (output != null && !output.trim().isEmpty()) {
            Path target = Paths.get(output).toAbsolutePath().normalize();
            Files.createDirectories(target);
            // Each run gets a new directory: historical/sealed artifacts are never overwritten.
            Path run = Files.createTempDirectory(target, "TEST-T06-real-exporter-");
            writeNew(run.resolve("TEST-long-text-real-exporter.xlsx"), longBytes);
            writeNew(run.resolve("TEST-empty-six-business-tables-real-exporter.xlsx"), emptyBytes);
            String provenance = ORIGIN + "\n"
                    + "Exporter: HrmPayrollGovernanceExportService.workbook(projected, 1L)\n"
                    + "Long fixture: 7000 repetitions of Chinese/emoji/six newlines; 2000 numbered basis clauses.\n"
                    + "Empty fixture: six empty business tables; exporter-generated EXPORT-METADATA remains on sheet 1.\n"
                    + "POI complete-field reconstruction passed before writing. Native Excel layout review is still required.\n"
                    + "TEST-long-text-real-exporter.xlsx SHA-256 " + sha256(longBytes) + "\n"
                    + "TEST-empty-six-business-tables-real-exporter.xlsx SHA-256 " + sha256(emptyBytes) + "\n";
            writeNew(run.resolve("TEST-FIXTURE-PROVENANCE.txt"), provenance.getBytes(StandardCharsets.UTF_8));
            System.out.println("T06_TEST_FIXTURE_OUTPUT=" + run);
        }
    }

    private static Map<String, List<Map<String, Object>>> emptyTables() {
        Map<String, List<Map<String, Object>>> tables = new LinkedHashMap<>();
        for (String title : HrmPayrollGovernanceProjectionService.SHEETS) tables.put(title, new ArrayList<>());
        return tables;
    }

    private static byte[] exportAndReadBack(Map<String, List<Map<String, Object>>> tables) throws Exception {
        Map<String, Object> projected = new LinkedHashMap<>();
        projected.put("label", ORIGIN);
        projected.put("sheets", tables);
        byte[] bytes = HrmPayrollGovernanceExportService.workbook(projected, 1L);
        try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(HrmPayrollGovernanceProjectionService.SHEETS.size(), book.getNumberOfSheets());
            for (int n = 0; n < book.getNumberOfSheets(); n++) {
                Sheet sheet = book.getSheetAt(n);
                String title = HrmPayrollGovernanceProjectionService.SHEETS.get(n);
                assertEquals(title, sheet.getSheetName());
                Map<String, Map<String, StringBuilder>> actual = new LinkedHashMap<>();
                Map<String, Integer> parents = new LinkedHashMap<>();
                Map<String, Integer> nextPart = new LinkedHashMap<>();
                Map<String, Integer> totals = new LinkedHashMap<>();
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    String ref = row.getCell(2).getStringCellValue();
                    if ("EXPORT-METADATA".equals(ref)) continue;
                    if (!"DETAIL".equals(row.getCell(0).getStringCellValue())) {
                        parents.merge(ref, 1, Integer::sum);
                        continue;
                    }
                    String field = row.getCell(3).getStringCellValue();
                    String key = ref + "/" + field;
                    int part = (int) row.getCell(4).getNumericCellValue();
                    int total = (int) row.getCell(5).getNumericCellValue();
                    assertEquals(nextPart.getOrDefault(key, 1).intValue(), part, title + "/" + key);
                    assertTrue(total >= part);
                    Integer previous = totals.putIfAbsent(key, total);
                    if (previous != null) assertEquals(previous.intValue(), total);
                    nextPart.put(key, part + 1);
                    assertEquals(CellType.STRING, row.getCell(7).getCellType());
                    String text = row.getCell(7).getStringCellValue();
                    assertPairedSurrogates(text);
                    actual.computeIfAbsent(ref, k -> new LinkedHashMap<>())
                            .computeIfAbsent(field, k -> new StringBuilder()).append(text);
                }
                assertEquals(tables.get(title).size(), parents.size(), title);
                assertEquals(parents.keySet(), actual.keySet(), title);
                for (Map<String, Object> record : tables.get(title)) {
                    String ref = String.valueOf(record.get("记录号"));
                    assertEquals(1, parents.get(ref).intValue(), title + "/" + ref);
                    assertEquals(record.keySet(), actual.get(ref).keySet(), title + "/" + ref);
                    for (Map.Entry<String, Object> field : record.entrySet()) {
                        Object value = field.getValue();
                        String expected = value == null ? "" : value instanceof String ? (String) value : JsonUtils.toJsonString(value);
                        String key = ref + "/" + field.getKey();
                        assertEquals(expected, actual.get(ref).get(field.getKey()).toString(), title + "/" + key);
                        assertEquals(totals.get(key).intValue() + 1, nextPart.get(key).intValue());
                    }
                }
            }
        }
        return bytes;
    }

    private static void assertPairedSurrogates(String text) {
        for (int n = 0; n < text.length(); n++) {
            char value = text.charAt(n);
            if (Character.isHighSurrogate(value)) {
                assertTrue(n + 1 < text.length() && Character.isLowSurrogate(text.charAt(n + 1)), "split emoji");
                n++;
            } else assertFalse(Character.isLowSurrogate(value), "unpaired low surrogate");
        }
    }

    private static void writeNew(Path path, byte[] bytes) throws Exception {
        Files.write(path, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    private static String sha256(byte[] bytes) throws Exception {
        StringBuilder hex = new StringBuilder();
        for (byte value : MessageDigest.getInstance("SHA-256").digest(bytes)) hex.append(String.format("%02x", value & 0xff));
        return hex.toString();
    }
}
