package cn.iocoder.yudao.framework.common.util.json.databind;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 基于时间戳的 LocalDateTime 反序列化器
 *
 * @author 老五
 */
public class TimestampLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    public static final TimestampLocalDateTimeDeserializer INSTANCE = new TimestampLocalDateTimeDeserializer();

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // Keep the existing epoch-millisecond contract, but never coerce invalid strings to zero.
        if (p.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            try {
                return fromMillis(p.getLongValue());
            } catch (DateTimeException e) {
                throw ctxt.weirdNumberException(p.getLongValue(), LocalDateTime.class, "Invalid epoch milliseconds");
            }
        }
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            String value = p.getText().trim();
            try {
                if (value.matches("-?\\d+")) return fromMillis(Long.parseLong(value));
                String iso = value.replace(' ', 'T');
                try {
                    return LocalDateTime.parse(iso);
                } catch (DateTimeException e) {
                    return OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
                }
            } catch (DateTimeException | NumberFormatException e) {
                throw ctxt.weirdStringException(value, LocalDateTime.class,
                        "Expected epoch milliseconds or ISO date-time, for example 2026-10-01T00:00:00");
            }
        }
        return (LocalDateTime) ctxt.handleUnexpectedToken(LocalDateTime.class, p);
    }

    private LocalDateTime fromMillis(long value) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneId.systemDefault());
    }

}
