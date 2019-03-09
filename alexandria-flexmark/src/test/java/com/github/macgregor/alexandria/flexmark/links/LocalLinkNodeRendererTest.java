package com.github.macgregor.alexandria.flexmark.links;

import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.html.renderer.ResolvedLink;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.sequence.BasedSequenceImpl;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LocalLinkNodeRendererTest {

    private Link node = spy(new Link(
            BasedSequenceImpl.of("[link text](https://www.google.com)")));
    private NodeRendererContext context = mock(NodeRendererContext.class);
    private HtmlWriter html = mock(HtmlWriter.class);
    private Document document = mock(Document.class);

    @Before
    public void setup(){
        when(document.get(any())).thenReturn(false);
        when(html.srcPos(any())).thenReturn(html);
        when(html.withAttr((ResolvedLink) any())).thenReturn(html);
        when(html.tag(any())).thenReturn(html);
        when(context.isDoNotRenderLinks()).thenReturn(false);
        when(context.resolveLink(any(), any(), any()))
                .thenReturn(new ResolvedLink(LocalLinkExtension.RELATIVE_LINK, "https://www.google.com"));

    }

    @Test
    public void testRelativeLinkNodeRendererFactoryCreatesRelativeLinkNodeRenderer(){
        NodeRenderer nodeRenderer = new LocalLinkNodeRenderer.Factory().create(document);
        assertThat(nodeRenderer).isInstanceOf(LocalLinkNodeRenderer.class);
    }

    @Test
    public void testRelativeLinkNodeRendererRendersLink(){
        LocalLinkNodeRenderer nodeRenderer = new LocalLinkNodeRenderer();
        nodeRenderer.render(node, context, html);
        verify(context, times(1)).resolveLink(any(), any(), any());
        verify(html, times(1)).attr("href", "https://www.google.com");
        verify(context, times(1)).renderChildren(node);
        verify(html, times(1)).tag("/a");
    }

    @Test
    public void testRelativeLinkNodeRendererRendersNothingWhenContextDoNotRenderLinks(){
        when(context.isDoNotRenderLinks()).thenReturn(true);

        LocalLinkNodeRenderer nodeRenderer = new LocalLinkNodeRenderer();
        nodeRenderer.render(node, context, html);
        verify(context, times(0)).resolveLink(any(), any(), any());
    }

    @Test
    public void testRelativeLinkNodeRendererCreatesHandlers(){
        LocalLinkNodeRenderer localLinkNodeRenderer = spy(new LocalLinkNodeRenderer());
        doAnswer(invocation -> {
            return null;
        }).when(localLinkNodeRenderer)
                .render(any(), any(), any());

        Set<NodeRenderingHandler<?>> handlers = localLinkNodeRenderer.getNodeRenderingHandlers();
        assertThat(handlers.size()).isEqualTo(1);
        NodeRenderingHandler<?> handler = handlers.toArray(new NodeRenderingHandler<?>[]{})[0];
        assertThat(handler.getNodeType())
                .isEqualTo(Link.class);
        handler.render(mock(Link.class), mock(NodeRendererContext.class), mock(HtmlWriter.class));
        verify(localLinkNodeRenderer, times(1)).render(any(), any(), any());
    }


}
