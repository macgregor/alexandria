package com.github.macgregor.alexandria;

import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.*;
import com.vladsch.flexmark.html.*;
import com.vladsch.flexmark.html.renderer.*;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.html.Attributes;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.vladsch.flexmark.util.options.MutableDataSet;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.vladsch.flexmark.html.renderer.CoreNodeRenderer.CODE_CONTENT;
import static com.vladsch.flexmark.parser.Parser.FENCED_CODE_CONTENT_BLOCK;

/**
 * Configure Flexmark markdown library
 */
@Slf4j
public class Markdown {

    private static Parser parser;
    private static HtmlRenderer renderer;
    public static MutableDataSet options;

    /**
     * Configure common Flexmark options to be used by both the {@link Parser} and {@link HtmlRenderer} instances.
     *
     * @return  flexmark options
     */
    public static MutableDataSet options(){
        if(options == null){
            options = new MutableDataSet();
            options.set(Parser.EXTENSIONS, Arrays.asList(
                    AutolinkExtension.create(),
                    StrikethroughExtension.create(),
                    TaskListExtension.create(),
                    TablesExtension.create(),
                    JiveExtension.create()));

            options.setFrom(ParserEmulationProfile.GITHUB_DOC);
            options.set(FENCED_CODE_CONTENT_BLOCK, true)
                    .set(Parser.CODE_SOFT_LINE_BREAKS, true);
                    //.set(HtmlRenderer.SOFT_BREAK, "<br />\n"); this doesnt do what you want, stop setting it.
        }
        return options;
    }

    /**
     * Retrieve the {@link Parser}, creating it if it doesnt exist
     *
     * @return  flexmark parser
     */
    public static Parser parser(){
        if(parser == null){
            parser = Parser.builder(options()).build();
        }
        return parser;
    }

    /**
     * Retrieve the {@link HtmlRenderer}, creating it if it doesnt exist
     *
     * @return  flexmark html renderer
     */
    public  static HtmlRenderer renderer(){
        if(renderer == null){
            renderer = HtmlRenderer.builder(options()).build();
        }
        return renderer;
    }

    /**
     * Convert a source file into html
     *
     * Source and converted paths dont have to be absolute, but it yields best results and to less confusing errors
     * when they are absolute.
     *
     * @param source  Absolute path to the file to convert
     * @param converted  Absolute path to output the converted html file to
     * @throws IOException  file system errors
     */
    public static void toHtml(Path source, Path converted) throws IOException {
        Node document = parser().parseReader(new FileReader(source.toFile()));
        Resources.save(converted.toString(), renderer().render(document));
        log.debug(String.format("Converted %s to %s.",
                source.toAbsolutePath().toString(),
                converted.toAbsolutePath().toString()));
    }

    public static class JiveExtension implements HtmlRenderer.HtmlRendererExtension{

        @Override
        public void rendererOptions(final MutableDataHolder options) {

        }

        @Override
        public void extend(HtmlRenderer.Builder rendererBuilder, String rendererType) {
            rendererBuilder.attributeProviderFactory(JiveAttributeProvider.Factory());
            if (rendererType.equals("HTML")) {
                rendererBuilder.nodeRendererFactory(new JiveCodeBlockNodeRenderer.Factory());
            }
        }

        static JiveExtension create(){
            return new JiveExtension();
        }
    }

    public static class JiveCodeBlockNodeRenderer implements NodeRenderer {

        public JiveCodeBlockNodeRenderer(DataHolder options) {
        }

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

        private void render(CodeBlock node, NodeRendererContext context, HtmlWriter html) {
            renderRawWithHardBreaks(node, html);
        }

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

        private void renderRawWithHardBreaks(Block node, HtmlWriter html){
            html.raw("\n");
            for(BasedSequence line : node.getContentLines()){
                html.text(line.trimTailBlankLines().trimEOL()).raw("<br />\n");
            }
        }

        public static class Factory implements NodeRendererFactory {
            @Override
            public NodeRenderer create(final DataHolder options) {
                return new JiveCodeBlockNodeRenderer(options);
            }
        }
    }

    public static class JiveAttributeProvider implements AttributeProvider{

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
