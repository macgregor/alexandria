package com.github.macgregor.alexandria;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class AlexandriaConfigTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testLoadDateTimeIsConverter() throws IOException {
        AlexandriaConfig config = AlexandriaConfig.load("src/test/resources/config.properties");
        assertThat(config.getMetadata()).isPresent();
        assertThat(config.getMetadata().get().get(0).getCreatedOn()).isEqualTo("2018-06-22T18:42:59.652+0000");
    }

    private File createPropertiesFile(Properties properties) throws IOException {
        File f = folder.newFile();
        JavaPropsMapper propertyMapper = new JavaPropsMapper();
        propertyMapper.registerModule(new Jdk8Module());
        propertyMapper.writeValue(properties, f);
        return f;
    }

    private File createYamlPropertiesFile(Properties properties) throws IOException {
        File f = folder.newFile();

        JavaPropsMapper propertyMapper = new JavaPropsMapper();
        propertyMapper.registerModule(new Jdk8Module());
        AlexandriaConfig config = propertyMapper.readPropertiesAs(properties, AlexandriaConfig.class);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.registerModule(new Jdk8Module());
        mapper.writeValue(f, config);
        return f;
    }
}
