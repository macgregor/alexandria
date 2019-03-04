package com.github.macgregor.alexandria.markdown;

import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Context;
import com.github.macgregor.alexandria.Resources;
import com.github.macgregor.alexandria.remotes.TestData;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class JiveFlexmarkExtensionTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    public Context context;
    private JiveMarkdownConverter converter;

    @Before
    public void setup() throws Exception{
        context = TestData.minimalJiveContext(folder);
        converter = new JiveMarkdownConverter();
        converter.alexandriaContext(context);
    }

    @Test
    public void testJiveCodeBlocksRenderAddsStylingClasses() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```xml\nline1  \nline2  \n```");
        converter.convert(mock(Config.DocumentMetadata.class), markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<pre class=\"language-markup line-numbers\">");
    }

    @Test
    public void testJiveCodeBlocksRenderingEscapesHtmlChracters() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```xml  \n<node>line1</node>  \n<node>line2</node>  \n```");
        converter.convert(mock(Config.DocumentMetadata.class), markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("&lt;node&gt;line1&lt;/node&gt;");
    }

    @Test
    public void testJiveCodeBlocksRenderingAddsHardBreaks() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```xml  \n<node>line1</node>  \n<node>line2</node>  \n```");
        converter.convert(mock(Config.DocumentMetadata.class), markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<br />");
    }

    @Test
    public void testJiveIndentedCodeBlocksRenderingAddsHardBreaks() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "    ```xml\n    <node>line1</node>\n    <node>line2</node>\n    ```");
        converter.convert(mock(Config.DocumentMetadata.class), markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<br />");
    }

    @Test
    public void testJiveCodeBlockStyleChangesYamlToJavascript() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```yaml  \nline1  \nline2  \n```");
        converter.convert(mock(Config.DocumentMetadata.class), markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<pre class=\"language-javascript line-numbers\">");
    }

    @Test
    public void testJiveCodeBlockStyleChangesYmlToJavascript() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```yml\nline1  \nline2  \n```");
        converter.convert(mock(Config.DocumentMetadata.class), markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<pre class=\"language-javascript line-numbers\">");
    }

    @Test
    public void testJiveCodeBlockStyleChangesXmlToMarkup() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```xml\nline1  \nline2  \n```");
        converter.convert(mock(Config.DocumentMetadata.class), markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<pre class=\"language-markup line-numbers\">");
    }

    @Test
    public void testJiveCodeBlockNoLanguage() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```\nline1  \nline2  \n```");
        converter.convert(mock(Config.DocumentMetadata.class), markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<pre class=\"language-none line-numbers\">");
    }

    @Test
    public void testJiveCodeBlockStyleChangesHtmlToMarkup() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```html\nline1  \nline2  \n```");
        converter.convert(mock(Config.DocumentMetadata.class), markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<pre class=\"language-markup line-numbers\">");
    }

    @Test
    public void testJiveCodeBlockStyleNoLanguage() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```\nline1  \nline2  \n```");
        converter.convert(mock(Config.DocumentMetadata.class), markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<pre class=\"language-none line-numbers\">");
    }

    @Test
    public void testJiveCodeBlockStyleUnknownLanguage() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```unknown\nline1  \nline2  \n```");
        converter.convert(mock(Config.DocumentMetadata.class), markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<pre class=\"language-unknown line-numbers\">");
    }

    @Test
    public void testJiveTableRenderingAddsTableClasses() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "| col1 | col2 |\n| --- | --- |\n| foo | bar |");
        converter.convert(mock(Config.DocumentMetadata.class), markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));

        String html = Resources.load(out.toString());
        assertThat(html).contains("<table class=\"j-table jiveBorder\" style=\"border: 1px solid #c6c6c6;\">");
    }

    @Test
    public void testJiveTableRenderingAddsTableRowClasses() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "| col1 | col2 |\n| --- | --- |\n| foo | bar |");
        converter.convert(mock(Config.DocumentMetadata.class), markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));

        String html = Resources.load(out.toString());
        assertThat(html).contains("<tr style=\"background-color: #efefef;\">");
    }

    @Test
    public void testJiveTableAlignmentRendering() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "| col1 | col2 | col3 | col4 |\n| :--- | :---: | ---: | --- |\n| foo | bar | baz | bat |");
        converter.convert(mock(Config.DocumentMetadata.class), markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));

        String html = Resources.load(out.toString());
        assertThat(html).contains("<td align=\"left\" style=\"text-align: left;\">");
        assertThat(html).contains("<td align=\"center\" style=\"text-align: center;\">");
        assertThat(html).contains("<td align=\"right\" style=\"text-align: right;\">");
        assertThat(html).contains("<td>bat</td>");
    }

    @Test
    public void testJiveRelativeLinkResolvesRemoteUri() throws IOException, URISyntaxException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Config.DocumentMetadata metadata = TestData.minimalDocumentMetadata(context, markdown.toPath());
        Resources.save(markdown.getPath(), "[link text](./foo.md)");
        metadata.remoteUri(Optional.of(new URI("https://www.google.com")));
        converter.convert(metadata, markdown.toPath(), out);

        String html = Resources.load(out.toString());
        assertThat(html).contains("<p><a href=\"https://www.google.com\">link text</a></p>");
    }

    @Test
    public void testJiveRelativeLinkResolvesNoRemoteUri() throws IOException, URISyntaxException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Config.DocumentMetadata metadata = TestData.minimalDocumentMetadata(context, markdown.toPath());
        Resources.save(markdown.getPath(), "[link text](./foo.md)");
        converter.convert(metadata, markdown.toPath(), out);

        String html = Resources.load(out.toString());
        assertThat(html).contains("<p><a href=\"./foo.md\">link text</a></p>");
    }
}
