package com.github.macgregor.alexandria.remotes;

import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TableCell;
import com.vladsch.flexmark.ext.tables.TableHead;
import com.vladsch.flexmark.ext.tables.TableRow;
import com.vladsch.flexmark.html.*;
import com.vladsch.flexmark.html.renderer.*;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.html.Attributes;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import java.util.HashSet;
import java.util.Set;

import static com.vladsch.flexmark.html.renderer.CoreNodeRenderer.CODE_CONTENT;

/**
 * Provides parsing and rendering support for Jive styling eccentricities.
 */
public class JiveFlexmarkExtension implements HtmlRenderer.HtmlRendererExtension{
    @Override
    public void rendererOptions(final MutableDataHolder options) {

    }

    /**
     * Adds {@link JiveAttributeProvider} and {@link JiveCodeBlockNodeRenderer} to Flexmark configuration.
     *
     * @param rendererBuilder  {@link HtmlRenderer.Builder}
     * @param rendererType  type of the renderer, e.g. HTML
     */
    @Override
    public void extend(HtmlRenderer.Builder rendererBuilder, String rendererType) {
        rendererBuilder.attributeProviderFactory(JiveAttributeProvider.Factory());
        if (rendererType.equals("HTML")) {
            rendererBuilder.nodeRendererFactory(new JiveCodeBlockNodeRenderer.Factory());
        }
    }

    /**
     * Static initializer for {@link JiveFlexmarkExtension}
     * @return  new {@link JiveFlexmarkExtension}
     */
    public static JiveFlexmarkExtension create(){
        return new JiveFlexmarkExtension();
    }

    /**
     * Provides rendering support for atypical Jive styling
     */
    public static class JiveCodeBlockNodeRenderer implements NodeRenderer {

        private DataHolder options;

        public JiveCodeBlockNodeRenderer(DataHolder options) {
            this.options = options;
        }

        /**
         * Return {@link NodeRenderingHandler}'s to add to Flexmark's rendering engine.
         *
         * @return  set of {@link NodeRenderingHandler} to override default Flexmark rendering behavior
         */
        @Override
        public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
            HashSet<NodeRenderingHandler<?>> set = new HashSet<NodeRenderingHandler<?>>();
            set.add(new NodeRenderingHandler<CodeBlock>(CodeBlock.class,
                    (node, context, html) -> JiveCodeBlockNodeRenderer.this.render(node, context, html)));
            set.add(new NodeRenderingHandler<IndentedCodeBlock>(IndentedCodeBlock.class,
                    (node, context, html) -> JiveCodeBlockNodeRenderer.this.render(node, context, html)));
            set.add(new NodeRenderingHandler<FencedCodeBlock>(FencedCodeBlock.class,
                    (node, context, html) -> JiveCodeBlockNodeRenderer.this.render(node, context, html)));
            return set;
        }

        /**
         * Renders a {@link CodeBlock} with hardline breaks to preserve new lines in Jive
         *
         * @param node  {@link CodeBlock}
         * @param context  {@link NodeRendererContext}
         * @param html  {@link HtmlWriter}
         */
        private void render(CodeBlock node, NodeRendererContext context, HtmlWriter html) {
            renderRawWithHardBreaks(node, html);
        }

        /**
         * Renders a {@link FencedCodeBlock} with hardline breaks to preserve new lines in Jive
         *
         * @param node  {@link FencedCodeBlock}
         * @param context  {@link NodeRendererContext}
         * @param html  {@link HtmlWriter}
         */
        private void render(FencedCodeBlock node, NodeRendererContext context, HtmlWriter html) {
            html.line();
            html.srcPosWithTrailingEOL(node.getChars()).withAttr().tag("pre").openPre();

            BasedSequence info = node.getInfo();
            if (info.isNotNull() && !info.isBlank()) {
                int space = info.indexOf(' ');
                BasedSequence language;
                if (space == -1) {
                    language = info;
                } else {
                    language = info.subSequence(0, space);
                }
                html.attr("class", context.getHtmlOptions().languageClassPrefix + language.unescape());
            } else {
                String noLanguageClass = context.getHtmlOptions().noLanguageClass.trim();
                if (!noLanguageClass.isEmpty()) {
                    html.attr("class", noLanguageClass);
                }
            }

            html.srcPosWithEOL(node.getContentChars()).withAttr(CODE_CONTENT).tag("code");
            if (Parser.FENCED_CODE_CONTENT_BLOCK.getFrom(options)) {
                context.renderChildren(node);
            } else {
                renderRawWithHardBreaks(node, html);
            }
            html.tag("/code");
            html.tag("/pre").closePre();
            html.lineIf(context.getHtmlOptions().htmlBlockCloseTagEol);
        }

        /**
         * Renders a {@link IndentedCodeBlock} with hardline breaks to preserve new lines in Jive
         *
         * @param node  {@link IndentedCodeBlock}
         * @param context  {@link NodeRendererContext}
         * @param html  {@link HtmlWriter}
         */
        private void render(IndentedCodeBlock node, NodeRendererContext context, HtmlWriter html) {
            html.line();
            html.srcPosWithEOL(node.getChars()).withAttr().tag("pre").openPre();

            String noLanguageClass = context.getHtmlOptions().noLanguageClass.trim();
            if (!noLanguageClass.isEmpty()) {
                html.attr("class", noLanguageClass);
            }

            html.srcPosWithEOL(node.getContentChars()).withAttr(CODE_CONTENT).tag("code");
            if (Parser.FENCED_CODE_CONTENT_BLOCK.getFrom(options)) {
                context.renderChildren(node);
            } else {
                renderRawWithHardBreaks(node, html);
            }
            html.tag("/code");
            html.tag("/pre").closePre();
            html.lineIf(context.getHtmlOptions().htmlBlockCloseTagEol);
        }

        /**
         * Render each line of html with a hard break {@code <br />} at the end
         *
         * @param node  {@link Block}
         * @param html  {@link HtmlWriter}
         */
        private void renderRawWithHardBreaks(Block node, HtmlWriter html){
            html.raw("\n");
            for(BasedSequence line : node.getContentLines()){
                html.text(line.trimTailBlankLines().trimEOL()).raw("<br />\n");
            }
        }

        /**
         * Factory used by Flexmark to instantiate a {@link JiveCodeBlockNodeRenderer}
         */
        public static class Factory implements NodeRendererFactory {
            @Override
            public NodeRenderer create(final DataHolder options) {
                return new JiveCodeBlockNodeRenderer(options);
            }
        }
    }

    /**
     * {@link AttributeProvider} to override simple styling markup.
     */
    public static class JiveAttributeProvider implements AttributeProvider{

        /**
         * Called by Flexmark renderers to add attributes to nodes.
         *
         * Used for things like overriding styling classes used. Currently overrides code block and table styling
         * to render nicely on Jive platforms
         *
         * @param node  {@link Node}
         * @param part  {@link AttributablePart}
         * @param attributes  {@link Attributes}
         */
        @Override
        public void setAttributes(Node node, AttributablePart part, Attributes attributes) {
            /*
            Jive syntax language options:
            None,
            Java
            JavaScript
            SQL
            HTML/xml
            CSS
            php
            Ruby
            Python
            C
            C#
            C++
             */
            if(node instanceof FencedCodeBlock){
                if(part == AttributablePart.NODE) {
                    String language = "none";
                    if (!((FencedCodeBlock) node).getInfo().isBlank()) {
                        language = ((FencedCodeBlock) node).getInfo().toString();
                        if(language.equals("yaml") || language.equals("yml")){
                            language = "javascript";
                        } else if(language.equals("html") || language.equals("xml")){
                            language = "markup";
                        }
                    }
                    attributes.replaceValue("class", String.format("language-%s line-numbers", language));
                } else{
                    attributes.replaceValue("class", null);
                }
            }
            if(node instanceof TableBlock){
                attributes.replaceValue("class", "j-table jiveBorder");
                attributes.replaceValue("style", "border: 1px solid #c6c6c6;");
            }
            if(node instanceof TableRow && node.getParent() instanceof TableHead){

                attributes.replaceValue("style", "background-color: #efefef;");
            }
            if(node instanceof TableCell && ((TableCell)node).getAlignment() != null){
                switch(((TableCell)node).getAlignment()){
                    case CENTER:
                        attributes.replaceValue("style", "text-align: center;");
                        break;
                    case LEFT:
                        attributes.replaceValue("style", "text-align: left;");
                        break;
                    case RIGHT:
                        attributes.replaceValue("style", "text-align: right;");
                        break;
                }
            }
        }

        static AttributeProviderFactory Factory(){
            return new IndependentAttributeProviderFactory(){
                @Override
                public AttributeProvider create(LinkResolverContext context) {
                    return new JiveAttributeProvider();
                }
            };
        }
    }
}
