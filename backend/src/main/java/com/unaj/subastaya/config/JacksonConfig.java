package com.unaj.subastaya.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;

@Configuration
public class JacksonConfig {

    @Bean
    public SimpleModule utcLocalDateTimeModule() {
        SimpleModule module = new SimpleModule("utcLocalDateTimeModule");
        module.addSerializer(LocalDateTime.class, new StdSerializer<>(LocalDateTime.class) {
            @Override
            public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider provider)
                    throws IOException {
                gen.writeString(value + "Z");
            }
        });
        return module;
    }
}
