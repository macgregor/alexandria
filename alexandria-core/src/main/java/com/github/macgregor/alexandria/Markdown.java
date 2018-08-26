package com.github.macgregor.alexandria;

import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

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
            options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));
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
}