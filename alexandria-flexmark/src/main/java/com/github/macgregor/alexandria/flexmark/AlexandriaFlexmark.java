package com.github.macgregor.alexandria.flexmark;

import com.vladsch.flexmark.Extension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.options.DataKey;
import com.vladsch.flexmark.util.options.MutableDataSet;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

import static com.vladsch.flexmark.parser.Parser.FENCED_CODE_CONTENT_BLOCK;

/**
 * Configure Flexmark markdown library, registering optional {@link Extension}s to customize
 * rendering and parsing of HTML.
 */
@Slf4j
@NoArgsConstructor
public class AlexandriaFlexmark {

    /** Default extensions added along with {@link AlexandriaFlexmark#registeredExtensions}. */
    public static final Extension[] DEFAULT_EXTENSIONS = new Extension[]{
            AutolinkExtension.create(),
            StrikethroughExtension.create(),
            TaskListExtension.create(),
            TablesExtension.create()
    };

    /** Default options added along with {@link AlexandriaFlexmark#options}. */
    public static final MutableDataSet DEFAULT_OPTIONS = (MutableDataSet) new MutableDataSet()
            .setFrom(ParserEmulationProfile.GITHUB_DOC)
            .set(FENCED_CODE_CONTENT_BLOCK, true)
            .set(Parser.CODE_SOFT_LINE_BREAKS, true);

    /** Additional extensions added at runtime. */
    protected Set<Extension> registeredExtensions = null;

    /** Additional options added at runtime. */
    protected MutableDataSet options = null;

    /** Flexmark html renderer used to create the HTML document. */
    protected HtmlRenderer htmlRenderer = null;

    /** Flexmark parser used to parse the markdown document. */
    protected Parser parser = null;

    /**
     * Register a Flexmark {@link Extension} to customize HTML rendering or parsing.
     *
     * Extensions must be registered before the global HtmlRenderer and Parser are instantiated
     * by calling {@link AlexandriaFlexmark#parser()} or {@link AlexandriaFlexmark#renderer()}
     *
     * @param extension the {@link Extension} to register
     */
    public void registerExtension(Extension extension){
        if(htmlRenderer != null || parser != null){
            throw new IllegalStateException("Extension registered after HtmlRenderer or Parser initialized.");
        }
        registeredExtensions().add(extension);
    }

    /**
     * Return the registered extensions, creating the underlying list and adding {@link AlexandriaFlexmark#DEFAULT_EXTENSIONS}
     * as needed.
     *
     * @return  initialized set of extensions that will be added to the {@link Parser} and {@link HtmlRenderer}
     */
    public Set<Extension> registeredExtensions(){
        if(registeredExtensions == null){
            registeredExtensions = new HashSet<>(4);
            for(Extension e : DEFAULT_EXTENSIONS){
                registeredExtensions.add(e);
            }
        }
        return registeredExtensions;
    }

    /**
     * Configure common Flexmark options to be used by both the {@link Parser} and {@link HtmlRenderer} instances.
     *
     * @return  flexmark options
     */
    public MutableDataSet options(){
        if(options == null) {
            options = new MutableDataSet();
            options.setFrom(DEFAULT_OPTIONS);
            options.set(Parser.EXTENSIONS, registeredExtensions());
        }
        return options;
    }

    /**
     * Set additional options to set on the {@link Parser} and {@link HtmlRenderer}
     * @param key
     * @param value
     * @param <T>
     */
    public <T> void setOption(DataKey<T> key, T value){
        options().set(key, value);
    }

    /**
     * Retrieve the {@link Parser}, creating it if it doesnt exist
     *
     * @return  flexmark parser
     */
    public Parser parser(){
        if(parser == null) {
            parser = Parser.builder(options()).build();
        }
        return parser;
    }

    /**
     * Retrieve the {@link HtmlRenderer}, creating it if it doesnt exist
     *
     * @return  flexmark html renderer
     */
    public  HtmlRenderer renderer(){
        if(htmlRenderer == null){
            htmlRenderer = HtmlRenderer.builder(options()).build();
        }
        return htmlRenderer;
    }

    /**
     * Reset the {@link AlexandriaFlexmark} instance's state, used for testing primarily.
     */
    public void reset(){
        options = null;
        registeredExtensions = null;
        htmlRenderer = null;
        parser = null;
    }
}
