package com.github.macgregor.alexandria;

import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

/**
 * Configure Flexmark markdown library
 */
public class Markdown {

    private static Parser parser;
    private static HtmlRenderer renderer;
    public static MutableDataSet options;

    public static MutableDataSet options(){
        if(options == null){
            options = new MutableDataSet();
            options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));
        }
        return options;
    }

    public static Parser parser(){
        if(parser == null){
            parser = Parser.builder(options()).build();
        }
        return parser;
    }

    public  static HtmlRenderer renderer(){
        if(renderer == null){
            renderer = HtmlRenderer.builder(options()).build();
        }
        return renderer;
    }

    /**
     * Convert a source file into html
     *
     * @param metadata {@link com.github.macgregor.alexandria.Config.DocumentMetadata} containing markdown source file to convert.
     * @throws IOException
     */
    public static void toHtml(Config.DocumentMetadata metadata) throws IOException {
        Node document = parser().parseReader(new FileReader(metadata.sourcePath().toFile()));
        Resources.save(metadata.convertedPath().get().toString(), renderer().render(document));
    }
}
