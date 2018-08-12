package com.github.macgregor.alexandria;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MarkdownConverterTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testMarkdownConverterInputDirsIgnoreFiles() throws IOException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme3.md");
        f.createNewFile();
        MarkdownConverter converter = new MarkdownConverter(Arrays.asList(subDir.getPath(), f.toString()));
        assertThat(converter.getDocumentDirectories()).containsExactly(subDir.toPath());
    }

    @Test(expected = FileNotFoundException.class)
    public void testMarkdownConverterInputDirsMustExist() throws IOException {
        MarkdownConverter converter = new MarkdownConverter(Arrays.asList("nope"));
    }

    @Test
    public void testConvertSingleDir() throws IOException, URISyntaxException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        f.createNewFile();
        MarkdownConverter converter = new MarkdownConverter(Arrays.asList(subDir.getPath()));
        assertThat(converter.convert().size()).isEqualTo(1);
        assertThat(Paths.get(subDir.toString(), "readme.html")).exists();
    }

    @Test
    public void testConvertMultipleDir() throws IOException, URISyntaxException {
        File subDir1 = folder.newFolder("foo");
        File f1 = new File(subDir1, "readme1.md");
        f1.createNewFile();
        File subDir2 = folder.newFolder("bar");
        File f2 = new File(subDir2, "readme2.md");
        f2.createNewFile();

        MarkdownConverter converter = new MarkdownConverter(Arrays.asList(subDir1.getPath(), subDir2.getPath()));
        assertThat(converter.convert().size()).isEqualTo(2);
        assertThat(Paths.get(subDir1.toString(), "readme1.html")).exists();
        assertThat(Paths.get(subDir2.toString(), "readme2.html")).exists();
    }

    @Test
    public void testConvertOverrideOutputDir() throws IOException, URISyntaxException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        f.createNewFile();
        File outDir = folder.newFolder("output");

        MarkdownConverter converter = new MarkdownConverter(Arrays.asList(subDir.getPath()), outDir.getPath());
        assertThat(converter.convert().size()).isEqualTo(1);
        assertThat(Paths.get(outDir.toString(), "readme.html")).exists();
    }

    @Test
    public void testConvertCreatesOutputDir() throws IOException, URISyntaxException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        f.createNewFile();
        File outDir = new File(folder.getRoot(), "output");

        MarkdownConverter converter = new MarkdownConverter(Arrays.asList(subDir.getPath()), outDir.getPath());
        assertThat(converter.convert().size()).isEqualTo(1);
        assertThat(Paths.get(outDir.toString(), "readme.html")).exists();
    }

    @Test
    public void testConvertHeadersToHtml() throws IOException, URISyntaxException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        Resources.save(f.getPath(), "# Header");
        File outDir = folder.newFolder("output");

        MarkdownConverter converter = new MarkdownConverter(Arrays.asList(subDir.getPath()), outDir.getPath());
        assertThat(converter.convert().size()).isEqualTo(1);
        assertThat(Paths.get(outDir.toString(), "readme.html")).exists();
        assertThat(Resources.load(Paths.get(outDir.toString(), "readme.html").toString())).isEqualTo("<h1>Header</h1>\n");
    }

    @Test
    public void testConvertStrikethroughToHtml() throws IOException, URISyntaxException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        Resources.save(f.getPath(), "~~strikethrough~~");
        File outDir = folder.newFolder("output");

        MarkdownConverter converter = new MarkdownConverter(Arrays.asList(subDir.getPath()), outDir.getPath());
        assertThat(converter.convert().size()).isEqualTo(1);
        assertThat(Paths.get(outDir.toString(), "readme.html")).exists();
        assertThat(Resources.load(Paths.get(outDir.toString(), "readme.html").toString())).isEqualTo("<p><del>strikethrough</del></p>\n");
    }

    @Test
    public void testConvertExtractedMetadataSetsSource() throws IOException, URISyntaxException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        Resources.save(f.getPath(), "~~strikethrough~~");
        File outDir = folder.newFolder("output");

        MarkdownConverter converter = new MarkdownConverter(Arrays.asList(subDir.getPath()), outDir.getPath());
        List<AlexandriaConfig.DocumentMetadata> metadata = converter.convert();
        assertThat(metadata.size()).isEqualTo(1);
        assertThat(metadata.get(0).sourcePath()).isEqualTo(f.toPath());
    }

    @Test
    public void testConvertExtractedMetadataSetsConverted() throws IOException, URISyntaxException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        Resources.save(f.getPath(), "~~strikethrough~~");
        File outDir = folder.newFolder("output");

        MarkdownConverter converter = new MarkdownConverter(Arrays.asList(subDir.getPath()), outDir.getPath());
        List<AlexandriaConfig.DocumentMetadata> metadata = converter.convert();
        assertThat(metadata.size()).isEqualTo(1);
        assertThat(metadata.get(0).convertedPath().get()).isEqualTo(Paths.get(outDir.toString(), "readme.html"));
    }
}
