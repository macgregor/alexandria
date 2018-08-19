package com.github.macgregor.alexandria;

import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Configure Flexmark markdown library
 */
public class Markdown {
    private static Logger log = LoggerFactory.getLogger(Markdown.class);

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
     * @param source
     * @param converted
     * @throws IOException
     */
    public static void toHtml(Path source, Path converted) throws IOException {
        Node document = parser().parseReader(new FileReader(source.toFile()));
        Resources.save(converted.toString(), renderer().render(document));
        log.debug(String.format("Converted %s to %s.",
                source.toAbsolutePath().toString(),
                converted.toAbsolutePath().toString()));
    }
}
