package com.github.macgregor.alexandria.markdown;

import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Context;
import com.github.macgregor.alexandria.Resources;
import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.flexmark.AlexandriaFlexmark;
import com.github.macgregor.alexandria.flexmark.links.LocalLinkExtension;
import com.vladsch.flexmark.util.ast.Document;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;

/**
 * {@link MarkdownConverter} implementation to properly format HTML for Jive when custom styling
 * is needed.
 */
@Slf4j
@AllArgsConstructor
@Data
public class JiveMarkdownConverter implements MarkdownConverter, Context.ContextAware {

    private Context context;
    private JiveFlexmarkExtension jiveFlexmarkExtension;
    private AlexandriaFlexmark flexmark = new AlexandriaFlexmark();

    public JiveMarkdownConverter(){
        jiveFlexmarkExtension = new JiveFlexmarkExtension();
        flexmark.registerExtension(jiveFlexmarkExtension);
    }

    /**
     * Uses Flexmark to convert markdown to html and resolve relative links to their remote URIs.
     *
     * @see {@link AlexandriaFlexmark}
     * @see {@link JiveFlexmarkExtension}
     *
     * @param metadata  metadata details about the document being converted
     * @param source  absolute path to the markdown source document that is being converted
     * @param converted  absolute path that should be used to write the converted document to
     * @throws AlexandriaException
     */
    @Override
    public void convert(Config.DocumentMetadata metadata, Path source, Path converted) throws AlexandriaException {
        if(context == null || flexmark.alexandriaContext() == null){
            throw new AlexandriaException.Builder()
                    .withMessage("Tried to convert documents without setting Alexandria context. JiveMarkdownConverter requires context to function properly.")
                    .metadataContext(metadata)
                    .build();
        }
        try {
            Document document = flexmark.parser().parseReader(new FileReader(source.toFile()));
            Resources.save(converted.toString(), flexmark.renderer().render(document));
        } catch(IOException e){
            throw new AlexandriaException.Builder()
                    .withMessage(String.format("Unable to convert {} to {}", source, converted))
                    .metadataContext(metadata)
                    .causedBy(e)
                    .build();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConvertedType convertedType() {
        return ConvertedType.HTML;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void alexandriaContext(Context context) {
        this.context = context;
        this.flexmark.alexandriaContext(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Context alexandriaContext() {
        return context;
    }
}
