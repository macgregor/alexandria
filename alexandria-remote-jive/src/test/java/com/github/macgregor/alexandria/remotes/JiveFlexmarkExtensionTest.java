package com.github.macgregor.alexandria.remotes;

import com.github.macgregor.alexandria.Context;
import com.github.macgregor.alexandria.Markdown;
import com.github.macgregor.alexandria.Resources;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class JiveFlexmarkExtensionTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    public Context context;

    @Before
    public void setup() throws Exception{
        context = TestData.minimalJiveContext(folder);
    }

    @Test
    public void testJiveCodeBlocksRenderAddsStylingClasses() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```xml\nline1  \nline2  \n```");
        Markdown.toHtml(context, markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<pre class=\"language-markup line-numbers\">");
    }

    @Test
    public void testJiveCodeBlocksRenderingEscapesHtmlChracters() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```xml  \n<node>line1</node>  \n<node>line2</node>  \n```");
        Markdown.toHtml(context, markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("&lt;node&gt;line1&lt;/node&gt;");
    }

    @Test
    public void testJiveCodeBlocksRenderingAddsHardBreaks() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```xml  \n<node>line1</node>  \n<node>line2</node>  \n```");
        Markdown.toHtml(context, markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<br />");
    }

    @Test
    public void testJiveIndentedCodeBlocksRenderingAddsHardBreaks() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "    ```xml\n    <node>line1</node>\n    <node>line2</node>\n    ```");
        Markdown.toHtml(context, markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<br />");
    }

    @Test
    public void testJiveCodeBlockStyleChangesYamlToJavascript() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```yaml  \nline1  \nline2  \n```");
        Markdown.toHtml(context, markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<pre class=\"language-javascript line-numbers\">");
    }

    @Test
    public void testJiveCodeBlockStyleChangesYmlToJavascript() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```yml\nline1  \nline2  \n```");
        Markdown.toHtml(context, markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<pre class=\"language-javascript line-numbers\">");
    }

    @Test
    public void testJiveCodeBlockStyleChangesXmlToMarkup() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```xml\nline1  \nline2  \n```");
        Markdown.toHtml(context, markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<pre class=\"language-markup line-numbers\">");
    }

    @Test
    public void testJiveCodeBlockNoLanguage() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```\nline1  \nline2  \n```");
        Markdown.toHtml(context, markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<pre class=\"language-none line-numbers\">");
    }

    @Test
    public void testJiveCodeBlockStyleChangesHtmlToMarkup() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```html\nline1  \nline2  \n```");
        Markdown.toHtml(context, markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<pre class=\"language-markup line-numbers\">");
    }

    @Test
    public void testJiveCodeBlockStyleNoLanguage() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "```\nline1  \nline2  \n```");
        Markdown.toHtml(context, markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));
        String html = Resources.load(out.toString());
        assertThat(html).contains("<pre class=\"language-none line-numbers\">");
    }

    @Test
    public void testJiveTableRenderingAddsTableClasses() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "| col1 | col2 |\n| --- | --- |\n| foo | bar |");
        Markdown.toHtml(context, markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));

        String html = Resources.load(out.toString());
        assertThat(html).contains("<table class=\"j-table jiveBorder\" style=\"border: 1px solid #c6c6c6;\">");
    }

    @Test
    public void testJiveTableRenderingAddsTableRowClasses() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "| col1 | col2 |\n| --- | --- |\n| foo | bar |");
        Markdown.toHtml(context, markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));

        String html = Resources.load(out.toString());
        assertThat(html).contains("<tr style=\"background-color: #efefef;\">");
    }

    @Test
    public void testJiveTableAlignmentRendering() throws IOException {
        File markdown = folder.newFile("foo.md");
        Path out = Paths.get(folder.getRoot().toString(), "foo.html");
        Resources.save(markdown.getPath(), "| col1 | col2 | col3 | col4 |\n| :--- | :---: | ---: | --- |\n| foo | bar | baz | bat |");
        Markdown.toHtml(context, markdown.toPath(), Paths.get(folder.getRoot().toString(), "foo.html"));

        String html = Resources.load(out.toString());
        assertThat(html).contains("<td align=\"left\" style=\"text-align: left;\">");
        assertThat(html).contains("<td align=\"center\" style=\"text-align: center;\">");
        assertThat(html).contains("<td align=\"right\" style=\"text-align: right;\">");
        assertThat(html).contains("<td>bat</td>");
    }
}
