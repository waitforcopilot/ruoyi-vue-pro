package cn.iocoder.yudao.framework.common.util.json.databind;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class PayrollDateContractTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()
            .addDeserializer(LocalDateTime.class, TimestampLocalDateTimeDeserializer.INSTANCE));

    @Test
    void acceptsLegacyMillisAndIsoWithoutSilentlyUsingEpochZero() throws Exception {
        LocalDateTime expected = LocalDateTime.of(2026,10,1,0,0);
        long millis = expected.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertEquals(expected, mapper.readValue(String.valueOf(millis), LocalDateTime.class));
        assertEquals(expected, mapper.readValue("\"" + millis + "\"", LocalDateTime.class));
        assertEquals(expected, mapper.readValue("\"2026-10-01T00:00:00\"", LocalDateTime.class));
        assertEquals(expected, mapper.readValue("\"2026-10-01 00:00:00\"", LocalDateTime.class));
        String offset = expected.atZone(ZoneId.systemDefault()).toOffsetDateTime().toString();
        assertEquals(expected, mapper.readValue("\"" + offset + "\"", LocalDateTime.class));
    }

    @Test
    void invalidValuesFailInsteadOfBecoming1970() {
        for (String value : new String[]{"\"garbage\"", "\"\"", "true", "1.5", "{}", "[]", "\"999999999999999999999999999\""}) {
            assertThrows(JsonMappingException.class, () -> mapper.readValue(value, LocalDateTime.class), value);
        }
    }

    @Test
    void fallbackJsonUtilsKeepsHistoricalSnapshotFormat() throws Exception {
        assertEquals("[2026,10,1]", JsonUtils.getObjectMapper().writeValueAsString(LocalDate.of(2026,10,1)));
        LocalDateTime value = LocalDateTime.of(2026,10,1,0,0);
        assertEquals(String.valueOf(value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()), JsonUtils.getObjectMapper().writeValueAsString(value));
        // Historical snapshots with date arrays remain readable.
        assertEquals(LocalDate.of(2026,10,1), JsonUtils.getObjectMapper().readValue("[2026,10,1]", LocalDate.class));
    }
}
