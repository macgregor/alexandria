package com.github.macgregor.alexandria.flexmark.links;

import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.html.CustomNodeRenderer;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.*;
import com.vladsch.flexmark.util.options.DataHolder;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * Renders the link resolved by {@link LocalLinkResolver} when writing the HTML document.
 */
@NoArgsConstructor
public class LocalLinkNodeRenderer implements NodeRenderer {

    @Override
    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        HashSet<NodeRenderingHandler<?>> set = new HashSet<NodeRenderingHandler<?>>();
        set.add(new NodeRenderingHandler<Link>(Link.class, new CustomNodeRenderer<Link>() {
            @Override
            public void render(Link node, NodeRendererContext context, HtmlWriter html) {
                LocalLinkNodeRenderer.this.render(node, context, html);
            }
        }));
        return set;
    }

    protected void render(Link node, NodeRendererContext context, HtmlWriter html) {
        if (context.isDoNotRenderLinks() != true) {
            ResolvedLink resolvedLink = context.resolveLink(LocalLinkExtension.RELATIVE_LINK, node.getUrl().unescape(), null);
            html.attr("href", resolvedLink.getUrl());
            html.srcPos(node.getChars()).withAttr(resolvedLink).tag("a");
            context.renderChildren(node);//html.text(node.getText().isNotNull() ? node.getText().toString() : node.getPageRef().toString());
            html.tag("/a");
        }
    }

    public static class Factory implements NodeRendererFactory {
        @Override
        public NodeRenderer create(final DataHolder options) {
            return new LocalLinkNodeRenderer();
        }
    }
}
