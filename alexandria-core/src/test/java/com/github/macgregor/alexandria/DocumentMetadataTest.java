package com.github.macgregor.alexandria;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DocumentMetadataTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testExtractMetadataParsesUrl() throws IOException, URISyntaxException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        Resources.save(f.getPath(), createMetadata("http://www.mojo.com/readme.html", "Test Doc", "tag1", "tag2"));
        DocumentMetadata extracted = DocumentMetadata.extract(f);

        assertThat(extracted.getRemote().get()).isEqualTo(new URI("http://www.mojo.com/readme.html"));
    }

    @Test(expected = MalformedURLException.class)
    public void testExtractMetadataParsesInvalidUrl() throws IOException, URISyntaxException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        Resources.save(f.getPath(), createMetadata("http://www.mojo.com/readme.html", "Test Doc", "tag1", "tag2"));
        DocumentMetadata extracted = DocumentMetadata.extract(f);

        assertThat(extracted.getRemote()).isEqualTo(new URL("invalid"));
    }

    @Test
    public void testExtractMetadataParsesTags() throws IOException, URISyntaxException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        Resources.save(f.getPath(), createMetadata("http://www.mojo.com/readme.html", "Test Doc", "tag1", "tag2"));
        DocumentMetadata extracted = DocumentMetadata.extract(f);

        assertThat(extracted.getTags().get()).containsExactlyInAnyOrder("tag1", "tag2");
    }

    @Test
    public void testExtractMetadataNoMetadata() throws IOException, URISyntaxException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        f.createNewFile();
        DocumentMetadata extracted = DocumentMetadata.extract(f);
    }

    @Test
    public void testExtractMetadataTrimsTags() throws IOException, URISyntaxException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        Resources.save(f.getPath(), createMetadata("http://www.mojo.com/readme.html", "Test Doc", "  tag1 ", "      tag2     "));
        DocumentMetadata extracted = DocumentMetadata.extract(f);

        assertThat(extracted.getTags().get()).containsExactlyInAnyOrder("tag1", "tag2");
    }

    @Test
    public void testExtractMetadataTrimsRemote() throws IOException, URISyntaxException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        Resources.save(f.getPath(), createMetadata("    http://www.mojo.com/readme.html  ", "Test Doc", "tag1", "tag2"));
        DocumentMetadata extracted = DocumentMetadata.extract(f);

        assertThat(extracted.getRemote().get()).isEqualTo(new URI("http://www.mojo.com/readme.html"));
    }

    @Test
    public void testExtractMetadataStoresExtraArgs() throws IOException, URISyntaxException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        Resources.save(f.getPath(), createMetadata(Collections.singletonMap("foo", "bar")));
        DocumentMetadata extracted = DocumentMetadata.extract(f);

        assertThat(extracted.getExtra()).containsKey("foo");
        assertThat(extracted.getExtra().get("foo")).isEqualTo("bar");
    }

    @Test
    public void testExtractParsesCreatedOnDateTime() throws IOException, URISyntaxException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        Resources.save(f.getPath(), createMetadata(Collections.singletonMap("createdOn", "2016-03-21T15:07:34.533+0000")));
        DocumentMetadata extracted = DocumentMetadata.extract(f);

        assertThat(extracted.getCreatedOn().get()).isEqualTo(ZonedDateTime.parse("2016-03-21T15:07:34.533+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
    }

    @Test
    public void testExtractParsesLastUpdatedDateTime() throws IOException, URISyntaxException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        Resources.save(f.getPath(), createMetadata(Collections.singletonMap("lastUpdated", "2016-03-21T15:07:34.533+0000")));
        DocumentMetadata extracted = DocumentMetadata.extract(f);

        assertThat(extracted.getLastUpdated().get()).isEqualTo(ZonedDateTime.parse("2016-03-21T15:07:34.533+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
    }

    private String createMetadata(String remote, String title, String... tags){
        return String.format("%s\nremote:%s\ntitle: %s\ntags:%s\n%s", DocumentMetadata.METADATA_START, remote, title, String.join(",", tags), DocumentMetadata.METADATA_END);
    }

    private String createMetadata(Map<String, String> args){
        List<String> extraArgs = new ArrayList();
        for(Map.Entry entry : args.entrySet()){
            extraArgs.add(String.format("%s: %s", entry.getKey(), entry.getValue()));
        }
        return String.format("%s\n%s\n%s",
                DocumentMetadata.METADATA_START,
                String.join("\n", extraArgs),
                DocumentMetadata.METADATA_END);
    }
}
