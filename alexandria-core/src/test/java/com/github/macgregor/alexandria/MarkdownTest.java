package com.github.macgregor.alexandria;

import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
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

        Markdown.toHtml(f.toPath(), Paths.get(outDir.toString(), "readme.html"));
        assertThat(Resources.load(Paths.get(outDir.toString(), "readme.html").toString())).isEqualTo("<h1>Header</h1>\n");
    }

    @Test
    public void testConvertStrikethroughToHtml() throws IOException, URISyntaxException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        Resources.save(f.getPath(), "~~strikethrough~~");
        File outDir = folder.newFolder("output");


        Markdown.toHtml(f.toPath(), Paths.get(outDir.toString(), "readme.html"));
        assertThat(Resources.load(Paths.get(outDir.toString(), "readme.html").toString())).isEqualTo("<p><del>strikethrough</del></p>\n");
    }

    @Test
    public void testJiveCodeBlocksRenderAddsStylingClasses() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```xml\nline1  \nline2  \n```");
        Markdown.toHtml(markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<pre class=\"language-markup line-numbers\">");
        log.info(html);
    }

    @Test
    public void testJiveCodeBlocksRenderingEscapesHtmlChracters() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```xml  \n<node>line1</node>  \n<node>line2</node>  \n```");
        Markdown.toHtml(markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("&lt;node&gt;line1&lt;/node&gt;");
    }

    @Test
    public void testJiveCodeBlocksRenderingAddsHardBreaks() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```xml  \n<node>line1</node>  \n<node>line2</node>  \n```");
        Markdown.toHtml(markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<br />");
        log.info(html);
    }

    @Test
    public void testJiveIndentedCodeBlocksRenderingAddsHardBreaks() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "    ```xml\n    <node>line1</node>\n    <node>line2</node>\n    ```");
        Markdown.toHtml(markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<br />");
        log.info(html);
    }

    @Test
    public void testJiveCodeBlockStyleChangesYamlToJavascript() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```yaml  \nline1  \nline2  \n```");
        Markdown.toHtml(markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<pre class=\"language-javascript line-numbers\">");
        log.info(html);
    }

    @Test
    public void testJiveCodeBlockStyleChangesYmlToJavascript() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```yml\nline1  \nline2  \n```");
        Markdown.toHtml(markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<pre class=\"language-javascript line-numbers\">");
        log.info(html);
    }

    @Test
    public void testJiveCodeBlockStyleChangesXmlToMarkup() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```xml\nline1  \nline2  \n```");
        Markdown.toHtml(markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<pre class=\"language-markup line-numbers\">");
        log.info(html);
    }

    @Test
    public void testJiveCodeBlockStyleChangesHtmlToMarkup() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```html\nline1  \nline2  \n```");
        Markdown.toHtml(markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<pre class=\"language-markup line-numbers\">");
        log.info(html);
    }

    @Test
    public void testJiveCodeBlockStyleNoLanguage() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```\nline1  \nline2  \n```");
        Markdown.toHtml(markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<pre class=\"language-none line-numbers\">");
        log.info(html);
    }

    @Test
    public void testJiveTableRenderingAddsTableClasses() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "| col1 | col2 |\n| --- | --- |\n| foo | bar |");
        Markdown.toHtml(markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));

        String html = Resources.load(out.toString());
        assertThat(html).contains("<table class=\"j-table jiveBorder\" style=\"border: 1px solid #c6c6c6;\">");
    }

    @Test
    public void testJiveTableRenderingAddsTableRowClasses() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "| col1 | col2 |\n| --- | --- |\n| foo | bar |");
        Markdown.toHtml(markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));

        String html = Resources.load(out.toString());
        assertThat(html).contains("<tr style=\"background-color: #efefef;\">");
    }

    @Test
    public void testJiveTableAlignmentRendering() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "| col1 | col2 | col3 | col4 |\n| :--- | :---: | ---: | --- |\n| foo | bar | baz | bat |");
        Markdown.toHtml(markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));

        String html = Resources.load(out.toString());
        assertThat(html).contains("<td align=\"left\" style=\"text-align: left;\">");
        assertThat(html).contains("<td align=\"center\" style=\"text-align: center;\">");
        assertThat(html).contains("<td align=\"right\" style=\"text-align: right;\">");
        assertThat(html).contains("<td>bat</td>");
    }
}
