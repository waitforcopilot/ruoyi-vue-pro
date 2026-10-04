package cn.iocoder.yudao.module.hrm.service.payroll.intake;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.intake.*;
import java.math.*;
import java.nio.*;
import java.nio.charset.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Bounded, strict UTF-8 CSV precheck. It never evaluates formulas or computes payroll. */
public final class HrmPayrollCsvValidator {
    public static final int MAX_BYTES = 1024 * 1024;
    public static final int MAX_ROWS = 500;
    private HrmPayrollCsvValidator() { }

    public static HrmPayrollPreviewResultVO validate(byte[] bytes, HrmPayrollContractSchemaVO schema,
            Long contractId, Integer version, HrmPayrollPreviewReqVO context) {
        HrmPayrollPreviewResultVO result = new HrmPayrollPreviewResultVO()
                .setEmployeeMatchEnabled(schema.getEmployeeField() != null && !schema.getEmployeeField().isEmpty());
        List<Record> records;
        try {
            String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            if (text.startsWith("\uFEFF")) text = text.substring(1);
            records = parse(text);
        } catch (CharacterCodingException e) {
            result.getGlobalIssues().add(issue(null, null, "ENCODING", "文件必须使用 UTF-8 编码"));
            return finish(result);
        } catch (CsvFormatException e) {
            result.getGlobalIssues().add(issue(e.line, null, e.code, e.getMessage()));
            return finish(result);
        }
        if (records.size() < 2 || !records.get(0).cells.equals(Arrays.asList(
                "#hrm-payroll-contract", String.valueOf(contractId), String.valueOf(version)))) {
            result.getGlobalIssues().add(issue(1, null, "TEMPLATE_VERSION", "模板标记或契约版本不符，请使用所选契约的模板"));
            return finish(result);
        }
        List<String> headers = records.get(1).cells;
        Set<String> expected = schema.getFields().stream().map(HrmPayrollContractSchemaVO.Field::getKey)
                .collect(Collectors.toSet());
        if (headers.size() != expected.size() || new HashSet<>(headers).size() != headers.size()
                || !new HashSet<>(headers).equals(expected)) {
            result.getGlobalIssues().add(issue(records.get(1).line, null, "HEADERS", "列名缺失、重复或包含未登记列，请保留模板列名"));
            return finish(result);
        }
        Map<String, Integer> positions = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) positions.put(headers.get(i), i);
        Map<String, List<HrmPayrollPreviewResultVO.Row>> keys = new HashMap<>();
        for (int i = 2; i < records.size(); i++) {
            Record record = records.get(i);
            HrmPayrollPreviewResultVO.Row row = new HrmPayrollPreviewResultVO.Row().setLine(record.line);
            result.getRows().add(row);
            if (record.cells.size() != headers.size()) {
                row.getIssues().add(issue(row.getLine(), null, "COLUMN_COUNT", "该行列数与模板不一致"));
                continue;
            }
            for (HrmPayrollContractSchemaVO.Field field : schema.getFields()) {
                String raw = record.cells.get(positions.get(field.getKey())).trim();
                if (raw.isEmpty()) {
                    row.getValues().put(field.getKey(), null);
                    if (Boolean.TRUE.equals(field.getRequired()))
                        row.getIssues().add(issue(row.getLine(), field.getKey(), "REQUIRED", "必填值缺失"));
                    continue;
                }
                try {
                    row.getValues().put(field.getKey(), normalize(raw, field));
                } catch (IllegalArgumentException e) {
                    row.getIssues().add(issue(row.getLine(), field.getKey(), "TYPE", "值不符合字段类型、长度或小数位约束"));
                }
            }
            String period = row.getValues().get(schema.getPeriodField());
            if (period != null) {
                LocalDate date = LocalDate.parse(period);
                if (date.isBefore(context.getPeriodStart()) || date.isAfter(context.getPeriodEnd()))
                    row.getIssues().add(issue(row.getLine(), schema.getPeriodField(), "PERIOD", "日期不在本次声明期间内"));
            }
            String subject = row.getValues().get(schema.getSubjectField());
            if (subject != null && !subject.equals(context.getDeclaredScope()))
                row.getIssues().add(issue(row.getLine(), schema.getSubjectField(), "SCOPE", "主体值与本次声明范围不一致"));
            List<String> values = schema.getKeyFields().stream().map(row.getValues()::get).collect(Collectors.toList());
            if (values.stream().allMatch(Objects::nonNull))
                keys.computeIfAbsent(JsonUtils.toJsonString(values), key -> new ArrayList<>()).add(row);
        }
        for (List<HrmPayrollPreviewResultVO.Row> duplicates : keys.values()) if (duplicates.size() > 1)
            for (HrmPayrollPreviewResultVO.Row row : duplicates)
                row.getIssues().add(issue(row.getLine(), null, "DUPLICATE", "复合唯一键重复；相关行均需处理"));
        if (result.getRows().isEmpty())
            result.getGlobalIssues().add(issue(null, null, "EMPTY", "文件没有可预检的数据行"));
        return finish(result);
    }

    private static String normalize(String raw, HrmPayrollContractSchemaVO.Field field) {
        switch (field.getType()) {
            case "TEXT":
                if (raw.length() > (field.getMaxLength() == null ? 1024 : field.getMaxLength()))
                    throw new IllegalArgumentException();
                return raw;
            case "INTEGER":
                if (!raw.matches("[+-]?[0-9]+")) throw new IllegalArgumentException();
                BigInteger integer = new BigInteger(raw);
                if (integer.abs().toString().length() > 18) throw new IllegalArgumentException();
                return integer.toString();
            case "DECIMAL":
                if (!raw.matches("[+-]?[0-9]+(\\.[0-9]+)?")) throw new IllegalArgumentException();
                BigDecimal decimal = new BigDecimal(raw);
                if (decimal.precision() > 18 || decimal.scale() > field.getScale()) throw new IllegalArgumentException();
                return decimal.stripTrailingZeros().toPlainString();
            case "DATE":
                if (!raw.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new IllegalArgumentException();
                try { return LocalDate.parse(raw).toString(); }
                catch (java.time.DateTimeException e) { throw new IllegalArgumentException(); }
            default: throw new IllegalArgumentException();
        }
    }

    public static HrmPayrollPreviewResultVO finish(HrmPayrollPreviewResultVO result) {
        result.setRowCount(result.getRows().size());
        result.setValidCount(result.getGlobalIssues().isEmpty()
                ? (int) result.getRows().stream().filter(row -> row.getIssues().isEmpty()).count() : 0);
        result.setErrorCount(result.getGlobalIssues().size()
                + result.getRows().stream().mapToInt(row -> row.getIssues().size()).sum());
        return result;
    }

    public static HrmPayrollPreviewResultVO.Issue issue(Integer line, String field, String code, String message) {
        return new HrmPayrollPreviewResultVO.Issue().setLine(line).setField(field).setCode(code).setMessage(message);
    }

    private static class Record {
        private final int line;
        private final List<String> cells;
        private Record(int line, List<String> cells) { this.line = line; this.cells = cells; }
    }
    private static class CsvFormatException extends IllegalArgumentException {
        private final int line;
        private final String code;
        private CsvFormatException(int line, String code, String message) {
            super(message); this.line = line; this.code = code;
        }
    }
    private static List<Record> parse(String text) {
        List<Record> rows = new ArrayList<>();
        List<String> cells = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false, closed = false, content = false;
        int line = 1, start = 1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\0') throw new CsvFormatException(line, "CSV_FORMAT", "CSV 含不支持的控制字符");
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') { value.append('"'); i++; }
                    else { quoted = false; closed = true; }
                } else if (c == '\r' || c == '\n') {
                    if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                    value.append('\n'); line++;
                } else value.append(c);
            } else if (c == ',' || c == '\r' || c == '\n') {
                cells.add(value.toString()); value.setLength(0); closed = false;
                if (cells.size() > 32) throw new CsvFormatException(line, "CSV_LIMIT", "每行最多 32 列");
                if (c == ',') content = true;
                else {
                    if (content) rows.add(new Record(start, cells));
                    cells = new ArrayList<>(); content = false;
                    if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                    line++; start = line;
                }
            } else {
                if (closed || (c == '"' && value.length() != 0))
                    throw new CsvFormatException(line, "CSV_FORMAT", "CSV 引号格式不合法");
                content = true;
                if (c == '"') quoted = true; else value.append(c);
            }
            if (value.length() > 4096) throw new CsvFormatException(line, "CSV_LIMIT", "单元格超过技术长度上限");
            if (rows.size() > MAX_ROWS + 2) throw new CsvFormatException(line, "CSV_LIMIT", "每批最多 500 行");
        }
        if (quoted) throw new CsvFormatException(line, "CSV_FORMAT", "CSV 引号未闭合");
        if (content) { cells.add(value.toString()); rows.add(new Record(start, cells)); }
        if (cells.size() > 32 || rows.size() > MAX_ROWS + 2)
            throw new CsvFormatException(line, "CSV_LIMIT", "文件超过 500 行或 32 列上限");
        return rows;
    }
}
