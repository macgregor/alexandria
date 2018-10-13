package com.github.macgregor.alexandria;

import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class MarkdownTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testConvertHeadersToHtml() throws IOException, URISyntaxException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        Resources.save(f.getPath(), "# Header");
        File outDir = folder.newFolder("output");
        Context context = TestData.minimalContext(folder);

        Markdown.toHtml(context, f.toPath(), Paths.get(outDir.toString(), "readme.html"));
        assertThat(Resources.load(Paths.get(outDir.toString(), "readme.html").toString())).isEqualTo("<h1>Header</h1>\n");
    }

    @Test
    public void testConvertStrikethroughToHtml() throws IOException, URISyntaxException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        Resources.save(f.getPath(), "~~strikethrough~~");
        File outDir = folder.newFolder("output");
        Context context = TestData.minimalContext(folder);

        Markdown.toHtml(context, f.toPath(), Paths.get(outDir.toString(), "readme.html"));
        assertThat(Resources.load(Paths.get(outDir.toString(), "readme.html").toString())).isEqualTo("<p><del>strikethrough</del></p>\n");
    }
}
