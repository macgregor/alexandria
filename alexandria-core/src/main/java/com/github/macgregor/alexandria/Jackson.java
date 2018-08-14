package com.github.macgregor.alexandria;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Jackson {
    private static ObjectMapper yamlMapper;
    private static ObjectMapper jsonMapper;

    public static ObjectMapper yamlMapper(){
        if(yamlMapper == null){
            yamlMapper = configureMapper(new ObjectMapper(new YAMLFactory()));
        }
        return yamlMapper;
    }

    public static ObjectReader yamlReader(){
        return yamlMapper().reader();
    }

    public static ObjectWriter yamlWriter(){
        return yamlMapper().writer();
    }

    public static ObjectMapper jsonMapper(){
        if(jsonMapper == null){
            jsonMapper = configureMapper(new ObjectMapper());
        }
        return jsonMapper;
    }

    public static ObjectReader jsonReader(){
        return jsonMapper().reader();
    }

    public static ObjectWriter jsonWriter(){
        return jsonMapper().writer();
    }

    public static class ZonedDateTimeDeserializer extends JsonDeserializer<ZonedDateTime> {

        private DateTimeFormatter dateTimeFormatter;

        public ZonedDateTimeDeserializer(DateTimeFormatter dateTimeFormatter){
            this.dateTimeFormatter = dateTimeFormatter;
        }

        @Override
        public ZonedDateTime deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                JsonProcessingException {
            return ZonedDateTime.parse(jp.getText(), dateTimeFormatter);
        }
    }

    public static class ZonedDateTimeSerializer extends JsonSerializer<ZonedDateTime> {

        private DateTimeFormatter dateTimeFormatter;

        public ZonedDateTimeSerializer(DateTimeFormatter dateTimeFormatter){
            this.dateTimeFormatter = dateTimeFormatter;
        }

        @Override
        public void serialize(ZonedDateTime value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
                JsonProcessingException {
            jgen.writeString(value.format(dateTimeFormatter));
        }
    }

    protected static ObjectMapper configureMapper(ObjectMapper mapper){
        Module jdk8Module = new Jdk8Module();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.registerModule(java8TimeModule());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.registerModule(jdk8Module);
        return mapper;
    }

    protected static JavaTimeModule java8TimeModule(){
        ZonedDateTimeDeserializer dateTimeDeserializer = new ZonedDateTimeDeserializer(DateTimeFormatter.ofPattern(Config.ALEXANDRIA_DATETIME_PATTERN));
        ZonedDateTimeSerializer dateTimeSerializer = new ZonedDateTimeSerializer(DateTimeFormatter.ofPattern(Config.ALEXANDRIA_DATETIME_PATTERN));

        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addDeserializer(ZonedDateTime.class, dateTimeDeserializer);
        javaTimeModule.addSerializer(ZonedDateTime.class, dateTimeSerializer);
        return javaTimeModule;
    }
}
