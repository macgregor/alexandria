package com.github.macgregor.alexandria.flexmark.links;

import com.github.macgregor.alexandria.Context;
import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.markdown.LinkResolver;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URI;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

public class LocalLinkExtensionTest {

    private LinkResolver alexandriaFlexmarkLinkResolver = mock(LinkResolver.class);
    private HtmlRenderer.Builder rendererBuilder = new HtmlRenderer.Builder();
    private Parser.Builder parserBuilder = new Parser.Builder();
    private LocalLinkExtension localLinkExtension = new LocalLinkExtension(alexandriaFlexmarkLinkResolver);

    @Test
    public void testRelativeLinkResolverExtensionRendererExtensionHtmlRenderType(){
        rendererBuilder.set(HtmlRenderer.TYPE, "HTML");
        localLinkExtension.extend(rendererBuilder, "");
        assertThat(localLinkExtension.getRelativeLinkNodeRendererFactory()).isNotNull();
        assertThat(localLinkExtension.getRelativeLinkResolverFactory()).isNotNull();
    }

    @Test
    public void testRelativeLinkResolverExtensionRendererExtensionNonHtmlRenderType(){
        rendererBuilder.set(HtmlRenderer.TYPE, "FOO");
        localLinkExtension.extend(rendererBuilder, "");
        assertThat(localLinkExtension.getRelativeLinkNodeRendererFactory()).isNull();
        assertThat(localLinkExtension.getRelativeLinkResolverFactory()).isNull();
    }

    @Test
    public void testRelativeLinkResolverExtensionExtendParser(){
        localLinkExtension.extend(parserBuilder);
        assertThat(localLinkExtension.getRelativeLinkRefProcessorFactory()).isNotNull();

    }

    @Test
    public void testRelativeLinkResolverExtensionRenderOptionsNoop(){
        LocalLinkExtension copy = new LocalLinkExtension(alexandriaFlexmarkLinkResolver);
        localLinkExtension.rendererOptions(mock(MutableDataHolder.class));
        assertThat(localLinkExtension).isEqualTo(copy);
    }

    @Test
    public void testRelativeLinkResolverExtensionParserOptionsNoop(){
        LocalLinkExtension copy = new LocalLinkExtension(alexandriaFlexmarkLinkResolver);
        localLinkExtension.parserOptions(mock(MutableDataHolder.class));
        assertThat(localLinkExtension).isEqualTo(copy);
    }

    @Test
    public void testRelatvieLinkResolverDoesntSetContextifResolverDoesntNeedIt(){
        Context context = mock(Context.class);
        localLinkExtension.alexandriaContext(context); //will throw error since mock cant be cast to Context
    }

    @Test
    public void testRelatvieLinkResolverDoesSetContextifResolverNeedIt(){
        Context context = mock(Context.class);
        MockResolver mockResolver = mock(MockResolver.class);
        LocalLinkExtension linkExtension = new LocalLinkExtension(mockResolver);
        linkExtension.alexandriaContext(context); //will throw error since mock cant be cast to Context
        verify(mockResolver, times(1)).alexandriaContext(context);
    }

    public abstract static class MockResolver implements LinkResolver, Context.ContextAware{}
}
