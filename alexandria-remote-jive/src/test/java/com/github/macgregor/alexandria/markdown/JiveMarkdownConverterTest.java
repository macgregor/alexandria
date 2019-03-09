package com.github.macgregor.alexandria.markdown;

import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Context;
import com.github.macgregor.alexandria.Resources;
import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.remotes.TestData;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class JiveMarkdownConverterTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private JiveMarkdownConverter jiveMarkdownConverter;
    private Context context;

    @Before
    public void setup(){
        this.jiveMarkdownConverter = new JiveMarkdownConverter();
        this.context = new Context();
        jiveMarkdownConverter.alexandriaContext(context);
    }

    @Test
    public void testConvertedTypeIsHtml(){
        assertThat(jiveMarkdownConverter.convertedType())
                .isEqualTo(MarkdownConverter.ConvertedType.HTML);
    }

    @Test
    public void testSettingAlexandriaContextAlsoSetsForAlexandriaFlexmark(){
        assertThat(jiveMarkdownConverter.alexandriaContext()).isEqualTo(context);
        assertThat(jiveMarkdownConverter.getFlexmark().alexandriaContext()).isEqualTo(context);
    }

    @Test
    public void testNewJiveMarkdownConverterAddsExtensionsToFlexmarkTest(){
        assertThat(jiveMarkdownConverter.getFlexmark().registeredExtensions())
                .contains(jiveMarkdownConverter.getJiveFlexmarkExtension());
    }

    @Test
    public void testConvertBeforeContextSetThrowsException(){
        jiveMarkdownConverter.alexandriaContext(null);
        assertThatThrownBy(() -> jiveMarkdownConverter.convert(
                mock(Config.DocumentMetadata.class), mock(Path.class), mock(Path.class)))
                .isInstanceOf(AlexandriaException.class);
    }

    @Test
    public void testConvertBeforeContextOnFlexmarkSetThrowsException(){
        jiveMarkdownConverter.getFlexmark().alexandriaContext(null);
        assertThatThrownBy(() -> jiveMarkdownConverter.convert(
                mock(Config.DocumentMetadata.class), mock(Path.class), mock(Path.class)))
                .isInstanceOf(AlexandriaException.class);
    }

    @Test
    public void testConvertHeadersToHtml() throws IOException, URISyntaxException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        Resources.save(f.getPath(), "# Header");
        File outDir = folder.newFolder("output");
        Context context = TestData.minimalContext(folder);

        jiveMarkdownConverter.convert(mock(Config.DocumentMetadata.class), f.toPath(), Paths.get(outDir.toString(), "readme.html"));
        assertThat(Resources.load(Paths.get(outDir.toString(), "readme.html").toString())).isEqualTo("<h1>Header</h1>\n");
    }

    @Test
    public void testConvertStrikethroughToHtml() throws IOException, URISyntaxException {
        File subDir = folder.newFolder("foo");
        File f = new File(subDir, "readme.md");
        Resources.save(f.getPath(), "~~strikethrough~~");
        File outDir = folder.newFolder("output");
        Context context = TestData.minimalContext(folder);

        jiveMarkdownConverter.convert(mock(Config.DocumentMetadata.class), f.toPath(), Paths.get(outDir.toString(), "readme.html"));
        assertThat(Resources.load(Paths.get(outDir.toString(), "readme.html").toString())).isEqualTo("<p><del>strikethrough</del></p>\n");
    }

    @Test
    public void testConvertIOExceptionWrappedInAlexandriaException(){
        assertThatThrownBy(() -> jiveMarkdownConverter.convert(
                mock(Config.DocumentMetadata.class), Paths.get("doesnt_exist"), mock(Path.class)))
                .isInstanceOf(AlexandriaException.class)
                .hasCauseInstanceOf(IOException.class);
    }
}
