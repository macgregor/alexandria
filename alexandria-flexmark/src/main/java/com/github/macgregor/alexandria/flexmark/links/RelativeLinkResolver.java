package com.github.macgregor.alexandria.flexmark.links;

import com.github.macgregor.alexandria.markdown.LinkResolver;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.html.LinkResolverFactory;
import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.html.renderer.LinkStatus;
import com.vladsch.flexmark.html.renderer.ResolvedLink;
import com.vladsch.flexmark.util.ast.Node;
import lombok.Getter;

import java.net.URI;
import java.util.Set;

/**
 * Used to delegate the resolution of links to the {@link LinkResolver}.
 */
@Getter
public class RelativeLinkResolver implements com.vladsch.flexmark.html.LinkResolver {
    private final LinkResolver alexandriaFlexmarkLinkResolver;
    private final RelativeLinkOptions options;

    public RelativeLinkResolver(LinkResolver alexandriaFlexmarkLinkResolver){
        this.alexandriaFlexmarkLinkResolver = alexandriaFlexmarkLinkResolver;
        this.options = new RelativeLinkOptions();
    }

    public RelativeLinkResolver(LinkResolverContext context, LinkResolver alexandriaFlexmarkLinkResolver) {
        this.alexandriaFlexmarkLinkResolver = alexandriaFlexmarkLinkResolver;
        this.options = new RelativeLinkOptions(context.getOptions());
    }

    @Override
    public ResolvedLink resolveLink(Node node, LinkResolverContext context, ResolvedLink link) {
        if(RelativeLinkExtension.RELATIVE_LINK.equals(link.getLinkType())){
            LinkStatus status = delegateLinkStatus((Link) node);
            if(LinkStatus.VALID.equals(status)){
                return delegateResolveLink((Link) node, link);
            } else{
                return link.withStatus(status);
            }
        }
        return link;
    }

    protected ResolvedLink delegateResolveLink(Link node, ResolvedLink link){
        try {
            URI resolved = alexandriaFlexmarkLinkResolver.resolve(
                    node.getText().toString(), node.getUrl().toString());
            return new ResolvedLink(RelativeLinkExtension.RELATIVE_LINK, resolved.toString(), null, LinkStatus.VALID);
        } catch(Exception e){
            link.withStatus(LinkStatus.UNKNOWN);
            return link;
        }
    }

    protected LinkStatus delegateLinkStatus(Link node){
        try {
            return alexandriaFlexmarkLinkResolver.isValid(node.getText().toString(), node.getUrl().toString()) ? LinkStatus.VALID : LinkStatus.INVALID;
        } catch(Exception e){
            return LinkStatus.UNKNOWN;
        }
    }

    public static class Factory implements LinkResolverFactory {
        private final LinkResolver alexandriaFlexmarkLinkResolver;

        public Factory(LinkResolver alexandriaFlexmarkLinkResolver){
            this.alexandriaFlexmarkLinkResolver = alexandriaFlexmarkLinkResolver;
        }

        @Override
        public Set<Class<? extends LinkResolverFactory>> getAfterDependents() {
            return null;
        }

        @Override
        public Set<Class<? extends LinkResolverFactory>> getBeforeDependents() {
            return null;
        }

        @Override
        public boolean affectsGlobalScope() {
            return false;
        }

        @Override
        public com.vladsch.flexmark.html.LinkResolver create(LinkResolverContext context) {
            return new RelativeLinkResolver(context, alexandriaFlexmarkLinkResolver);
        }
    }
}
