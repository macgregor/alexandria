package com.github.macgregor.alexandria.markdown;

import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import lombok.Getter;
import lombok.ToString;

import java.nio.file.Path;

/**
 * Abstraction around the markdown conversion that alexandria-core can use in its
 * general processing while delegating the actual conversion details to other modules such
 * as the alexandria-remote-jive implementation which uses Flexmark and some custom extensions
 * to improve rendering on the Jive platform.
 *
 * @see NoopMarkdownConverter
 */
public interface MarkdownConverter {

    /**
     * Called for each document that needs converting after its converted path has been calculated.
     *
     * @param metadata  metadata details about the document being converted
     * @param source  absolute path to the markdown source document that is being converted
     * @param converted  absolute path that should be used to write the converted document to
     * @throws AlexandriaException  the document couldnt be converted for some reason
     */
    void convert(Config.DocumentMetadata metadata, Path source, Path converted) throws AlexandriaException;

    /**
     * Type of documents the {@link MarkdownConverter} produces. Used when determining what the converted path
     * for a document should be.
     *
     * @return  converted document type
     */
    default ConvertedType convertedType(){
        return ConvertedType.HTML;
    }

    /**
     * Class containing acceptable converted document types, including their expected file extensions.
     */
    @Getter
    @ToString
    enum ConvertedType{
        MARKDOWN("md"), HTML("html");

        /** File extension of the document type, not including the "." */
        private String fileExtension;

        ConvertedType(String fileExtension){
            this.fileExtension = fileExtension;
        }
    }
}
