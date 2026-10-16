package cn.iocoder.yudao.framework.jackson.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class YudaoJacksonDateContractTest {
    @Test
    void builderAndBootModuleAgreeOnDateContract() throws Exception {
        YudaoJacksonAutoConfiguration configuration = new YudaoJacksonAutoConfiguration();
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        configuration.ldtEpochMillisCustomizer().customize(builder);
        ObjectMapper mapper = builder.build();
        for (int registration = 0; registration < 2; registration++) {
            assertEquals("\"2026-01-01\"", mapper.writeValueAsString(LocalDate.of(2026,1,1)));
            assertEquals(LocalDateTime.of(2026,10,1,0,0), mapper.readValue("\"2026-10-01T00:00:00\"", LocalDateTime.class));
            mapper.registerModule(configuration.timestampSupportModuleBean());
        }
        configuration.jsonUtils(mapper);
        assertEquals("[2026,1,1]", cn.iocoder.yudao.framework.common.util.json.JsonUtils.getObjectMapper().writeValueAsString(LocalDate.of(2026,1,1)));
        assertEquals("\"2026-01-01\"", mapper.writeValueAsString(LocalDate.of(2026,1,1)));
    }

    public static class DateBody { public LocalDateTime date; }

    @org.springframework.web.bind.annotation.RestController
    public static class DateController {
        @org.springframework.web.bind.annotation.PostMapping("/test-date")
        public cn.iocoder.yudao.framework.common.pojo.CommonResult<DateBody> date(
                @org.springframework.web.bind.annotation.RequestBody DateBody body) {
            return cn.iocoder.yudao.framework.common.pojo.CommonResult.success(body);
        }
    }

    @Test
    void mvcBindingReturnsBadRequestForInvalidDatesAndJson() throws Exception {
        YudaoJacksonAutoConfiguration configuration = new YudaoJacksonAutoConfiguration();
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        configuration.ldtEpochMillisCustomizer().customize(builder);
        org.springframework.test.web.servlet.MockMvc mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(new DateController())
                .setControllerAdvice(new cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler("test", null))
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(builder.build()))
                .build();
        for (String body : new String[]{"{\"date\":true}", "{\"date\":1.5}", "{\"date\":{}}", "{\"date\":\"invalid-date\"}", "{"}) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/test-date")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(400));
        }
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/test-date")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"date\":\"2026-10-01T00:00:00\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(0));
    }
}
