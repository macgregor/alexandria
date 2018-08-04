package com.github.macgregor.alexandria;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class MetadataTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testExtractMetadataParsesUrl() throws IOException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        Resources.save(f.getPath(), createMetadata("http://www.mojo.com/readme.html", "tag1", "tag2"));
        Metadata extracted = Metadata.extract(f);

        assertThat(extracted.getRemote().get()).isEqualTo(new URL("http://www.mojo.com/readme.html"));
    }

    @Test(expected = MalformedURLException.class)
    public void testExtractMetadataParsesInvalidUrl() throws IOException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        Resources.save(f.getPath(), createMetadata("http://www.mojo.com/readme.html", "tag1", "tag2"));
        Metadata extracted = Metadata.extract(f);

        assertThat(extracted.getRemote()).isEqualTo(new URL("invalid"));
    }

    @Test
    public void testExtractMetadataParsesTags() throws IOException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        Resources.save(f.getPath(), createMetadata("http://www.mojo.com/readme.html", "tag1", "tag2"));
        Metadata extracted = Metadata.extract(f);

        assertThat(extracted.getTags().get()).containsExactlyInAnyOrder("tag1", "tag2");
    }

    @Test
    public void testExtractMetadataNoMetadata() throws IOException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        f.createNewFile();
        Metadata extracted = Metadata.extract(f);
    }

    @Test
    public void testExtractMetadataTrimsTags() throws IOException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        Resources.save(f.getPath(), createMetadata("http://www.mojo.com/readme.html", "  tag1 ", "      tag2     "));
        Metadata extracted = Metadata.extract(f);

        assertThat(extracted.getTags().get()).containsExactlyInAnyOrder("tag1", "tag2");
    }

    @Test
    public void testExtractMetadataTrimsRemote() throws IOException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        Resources.save(f.getPath(), createMetadata("    http://www.mojo.com/readme.html  ", "tag1", "tag2"));
        Metadata extracted = Metadata.extract(f);

        assertThat(extracted.getRemote().get()).isEqualTo(new URL("http://www.mojo.com/readme.html"));
    }

    private String createMetadata(String remote, String... tags){
        return String.format("%s\nremote:%s\ntags:%s\n%s", Metadata.METADATA_START, remote, String.join(",", tags), Metadata.METADATA_END);
    }
}
