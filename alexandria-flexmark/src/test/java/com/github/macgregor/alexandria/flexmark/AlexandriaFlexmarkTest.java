package com.github.macgregor.alexandria.flexmark;

import com.vladsch.flexmark.Extension;
import com.vladsch.flexmark.parser.Parser;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class AlexandriaFlexmarkTest {

    private AlexandriaFlexmark flexmark;

    @Before
    public void setup(){
        flexmark = new AlexandriaFlexmark();
    }

    @Test
    public void testAlexandriaFlexmarkRegisterExtension(){
        Extension extension = mock(Extension.class);
        flexmark.registerExtension(extension);
        assertThat(flexmark.registeredExtensions).isNotNull();
        assertThat(flexmark.registeredExtensions).contains(extension);
    }

    @Test
    public void testAlexandriaFlexmarkRegisterExtensionThrowsErrorAfterHtmlRendererRetrieved(){
        Extension extension = mock(Extension.class);
        flexmark.registerExtension(extension);
        flexmark.renderer();
        assertThatThrownBy(() -> flexmark.registerExtension(extension))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testAlexandriaFlexmarkRegisterExtensionThrowsErrorAfterParserRetrieved(){
        Extension extension = mock(Extension.class);
        flexmark.registerExtension(extension);
        flexmark.parser();
        assertThatThrownBy(() -> flexmark.registerExtension(extension))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testAlexandriaFlexmarkAddsDefaultExtensions(){
        assertThat(flexmark.registeredExtensions())
                .containsExactlyInAnyOrder(AlexandriaFlexmark.DEFAULT_EXTENSIONS);
    }

    @Test
    public void testAlexandriaFlexmarkOptionsAddsDefaultExtensions(){
        assertThat(flexmark.options().get(Parser.EXTENSIONS))
                .containsExactlyInAnyOrder(AlexandriaFlexmark.DEFAULT_EXTENSIONS);
    }

    @Test
    public void testAlexandriaFlexmarkReturnsStatisParserInstance(){
        assertThat(flexmark.parser()).isEqualTo(flexmark.parser());
    }

    @Test
    public void testAlexandriaFlexmarkReturnsStatisRendererInstance(){
        assertThat(flexmark.renderer()).isEqualTo(flexmark.renderer());
    }

    @Test
    public void testAlexandriaFlexmarkResetsExtension(){
        Extension extension = mock(Extension.class);
        flexmark.registerExtension(extension);
        flexmark.reset();
        assertThat(flexmark.registeredExtensions())
                .containsExactlyInAnyOrder(AlexandriaFlexmark.DEFAULT_EXTENSIONS);
    }

    @Test
    public void testAlexandriaFlexmarkResetsParser(){
        flexmark.parser();
        flexmark.reset();
        assertThat(flexmark.parser).isNull();
    }

    @Test
    public void testAlexandriaFlexmarkResetsRenderer(){
        flexmark.renderer();
        flexmark.reset();
        assertThat(flexmark.htmlRenderer).isNull();
    }
}
