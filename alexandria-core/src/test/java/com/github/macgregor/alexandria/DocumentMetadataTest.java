package com.github.macgregor.alexandria;

import org.junit.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class DocumentMetadataTest {

    @Test
    public void testEquals(){
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        metadata.title("title");

        Config.DocumentMetadata other = new Config.DocumentMetadata();
        other.sourcePath(Paths.get("foo"));
        other.title("title");

        assertThat(metadata).isEqualTo(other);
    }

    @Test
    public void testNotEquals(){
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        metadata.title("title1");

        Config.DocumentMetadata other = new Config.DocumentMetadata();
        other.sourcePath(Paths.get("foo"));
        other.title("title2");

        assertThat(metadata).isNotEqualTo(other);
    }

    @Test
    public void testEqualsOnlyCaresAboutTitleAndSourcePath(){
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        metadata.title("title");
        metadata.tags(Optional.of(Arrays.asList("foo")));

        Config.DocumentMetadata other = new Config.DocumentMetadata();
        other.sourcePath(Paths.get("foo"));
        other.title("title");
        metadata.tags(Optional.of(Arrays.asList("bar")));

        assertThat(metadata).isEqualTo(other);
    }

    @Test
    public void testToString(){
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        metadata.title("title");

        Config.DocumentMetadata other = new Config.DocumentMetadata();
        other.sourcePath(Paths.get("foo"));
        other.title("title");

        assertThat(metadata.toString()).isEqualTo(other.toString());
    }

}
