package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
/** Precision settings cannot silently truncate JSON fractions or coerce strings. */
public class ExplicitIntegerDeserializer extends JsonDeserializer<Integer> {
    @Override public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT)
            throw context.wrongTokenException(parser, Integer.class, JsonToken.VALUE_NUMBER_INT, "精度须为明确的整数");
        return parser.getIntValue();
    }
}
