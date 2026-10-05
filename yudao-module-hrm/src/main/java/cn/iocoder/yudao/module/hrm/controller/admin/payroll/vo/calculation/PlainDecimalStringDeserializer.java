package cn.iocoder.yudao.module.hrm.controller.admin.payroll.vo.calculation;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
/** Keep financial values as explicitly supplied strings, without scalar coercion. */
public class PlainDecimalStringDeserializer extends JsonDeserializer<String> {
    @Override public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() != JsonToken.VALUE_STRING)
            throw context.wrongTokenException(parser, String.class, JsonToken.VALUE_STRING, "计算输入和预期值须为明确的十进制字符串");
        return parser.getText();
    }
}
