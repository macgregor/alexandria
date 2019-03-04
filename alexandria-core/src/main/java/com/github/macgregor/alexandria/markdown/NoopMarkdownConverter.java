package com.github.macgregor.alexandria.markdown;

import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Resources;
import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

/**
 * Noop implementation of {@link MarkdownConverter} that just copies the markdown source document
 * to the converted path without any extra processing.
 *
 * Can be used for remotes that support markdown natively.
 */
@Slf4j
public class NoopMarkdownConverter implements MarkdownConverter {

    /**
     * {@inheritDoc}
     */
    @Override
    public void convert(Config.DocumentMetadata metadata, Path source, Path converted) throws AlexandriaException {
        log.debug("Noop - Converting {} to {}.", source, converted);
        try {
            Resources.save(converted.toString(), Resources.load(source.toString()));
        } catch(Exception e){
            throw new AlexandriaException.Builder()
                    .withMessage(String.format("Unexcepted error converting %s to %s",
                            source, convertedType().name().toLowerCase()))
                    .metadataContext(metadata)
                    .causedBy(e)
                    .build();
        }
        return;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConvertedType convertedType(){
        log.debug("Noop - Converted type will be {}.", ConvertedType.MARKDOWN);
        return ConvertedType.MARKDOWN;
    }
}
