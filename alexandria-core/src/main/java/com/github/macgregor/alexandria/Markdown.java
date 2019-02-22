package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.remotes.Remote;
import com.vladsch.flexmark.Extension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.options.MutableDataSet;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.vladsch.flexmark.parser.Parser.FENCED_CODE_CONTENT_BLOCK;

/**
 * Configure Flexmark markdown library
 */
@Slf4j
public class Markdown {

    private static Map<Remote, HtmlRenderer> renderers = new HashMap<>();
    private static Map<Remote, Parser> parsers = new HashMap<>();

    /**
     * Configure common Flexmark options to be used by both the {@link Parser} and {@link HtmlRenderer} instances.
     *
     * The remote is used to add custom rendering/parsing logic for remotes.
     *
     * @see Remote#htmlRenderer()
     *
     * @param remote  Remote implementation which may or may not have custom Flexmark extensions to add.
     * @return  flexmark options
     */
    public static MutableDataSet options(Remote remote){
        MutableDataSet options = new MutableDataSet();

        List<Extension> extensions = new ArrayList<>();
        extensions.add(AutolinkExtension.create());
        extensions.add(StrikethroughExtension.create());
        extensions.add(TaskListExtension.create());
        extensions.add(TablesExtension.create());

        if(remote.htmlRenderer().isPresent()){
            extensions.add(remote.htmlRenderer().get());
        }
        options.set(Parser.EXTENSIONS, extensions);

        options.setFrom(ParserEmulationProfile.GITHUB_DOC);
        options.set(FENCED_CODE_CONTENT_BLOCK, true)
                .set(Parser.CODE_SOFT_LINE_BREAKS, true);
                //.set(HtmlRenderer.SOFT_BREAK, "<br />\n"); this doesnt do what you want, stop setting it.
        return options;
    }

    /**
     * Retrieve the {@link Parser} for a given {@link Remote}, creating it if it doesnt exist
     *
     * @param remote
     * @return  flexmark parser
     */
    public static Parser parser(Remote remote){
        if(!parsers.containsKey(remote)){
            parsers.put(remote, Parser.builder(options(remote)).build());
        }
        return parsers.get(remote);
    }

    /**
     * Retrieve the {@link HtmlRenderer} for a given {@link Remote}, creating it if it doesnt exist
     *
     * @see Remote#htmlRenderer()
     *
     * @param remote
     * @return  flexmark html renderer
     */
    public  static HtmlRenderer renderer(Remote remote){
        if(!renderers.containsKey(remote)){
            renderers.put(remote, HtmlRenderer.builder(options(remote)).build());
        }
        return renderers.get(remote);
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
    public static void toHtml(Context context, Path source, Path converted) throws IOException {
        Remote remote = context.configureRemote();
        Document document = parser(remote).parseReader(new FileReader(source.toFile()));
        Resources.save(converted.toString(), renderer(remote).render(document));
        log.debug(String.format("Converted %s to %s.",
                source.toAbsolutePath().toString(),
                converted.toAbsolutePath().toString()));
    }
}
