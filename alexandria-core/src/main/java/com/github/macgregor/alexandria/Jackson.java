package com.github.macgregor.alexandria;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Static configuration of the Jackson parser
 *
 * Used by {@link com.github.macgregor.alexandria.remotes.Remote} implementations for parsing json requests and responses
 * as well as the saving/loading of the Alexandria {@link Context}.
 */
public class Jackson {
    private static ObjectMapper yamlMapper;
    private static ObjectMapper jsonMapper;

    /**
     * Retrieve the yaml mapper, creating it if it doesnt exist.
     *
     * @return  mapper configured for yaml
     */
    public static ObjectMapper yamlMapper(){
        if(yamlMapper == null){
            yamlMapper = configureMapper(new ObjectMapper(new YAMLFactory()));
        }
        return yamlMapper;
    }

    /**
     * Retrieve the json mapper, creating it if it doesnt exist.
     *
     * @return  mapper configured for json
     */
    public static ObjectMapper jsonMapper(){
        if(jsonMapper == null){
            jsonMapper = configureMapper(new ObjectMapper());
        }
        return jsonMapper;
    }

    /**
     * Converts a string to a {@link ZonedDateTime} instance using a provided {@link DateTimeFormatter}
     */
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

    /**
     * Converts a {@link ZonedDateTime} instance to a string using the provided {@link DateTimeFormatter}
     */
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

    /**
     * Converts a string into a {@link Path}, which Jackson wasnt always handling gracefully.
     */
    public static class PathDeserializer extends JsonDeserializer<Path> {

        @Override
        public Path deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return Paths.get(p.getText());
        }
    }

    /**
     * Converts a {@link Path} into a string, which Jackson wasnt always handling gracefully.
     */
    public static class PathSerializer extends JsonSerializer<Path> {

        @Override
        public void serialize(Path value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.toString());
        }
    }

    /**
     * Configure Jakson mapper with commong configurations; ignoring unknown properties, enable jdk8 feature, etc.
     *
     * @param mapper  mapper to be configured
     * @return  mapper with configuration added
     */
    protected static ObjectMapper configureMapper(ObjectMapper mapper){
        Module jdk8Module = new Jdk8Module();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.registerModule(jdk8Module);
        mapper.registerModule(java8TimeModule());
        mapper.registerModule(pathModule());
        return mapper;
    }

    /**
     * Configure Java 8 time module to allow use of newer, better date time features. Also add the {@link ZonedDateTime}
     * (de)serializers to the module.
     *
     * @return  java time module configured to use zoned date times
     */
    protected static JavaTimeModule java8TimeModule(){
        ZonedDateTimeDeserializer dateTimeDeserializer = new ZonedDateTimeDeserializer(DateTimeFormatter.ofPattern(Config.ALEXANDRIA_DATETIME_PATTERN));
        ZonedDateTimeSerializer dateTimeSerializer = new ZonedDateTimeSerializer(DateTimeFormatter.ofPattern(Config.ALEXANDRIA_DATETIME_PATTERN));

        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addDeserializer(ZonedDateTime.class, dateTimeDeserializer);
        javaTimeModule.addSerializer(ZonedDateTime.class, dateTimeSerializer);
        return javaTimeModule;
    }

    /**
     * Configure a new module for converting {@link Path} objects to/from JSON.
     *
     * @return  configured module that converts paths
     */
    protected static SimpleModule pathModule(){
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Path.class, new PathDeserializer());
        module.addSerializer(Path.class, new PathSerializer());
        return module;
    }
}
