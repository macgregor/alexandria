package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Slf4j
@ToString
@Getter @Setter @Accessors(fluent = true)
@NoArgsConstructor @AllArgsConstructor
public class AlexandriaConvert {

    @NonNull private Context context;

    /**
     * Convert HTML files from the files in the metadata index. Converted files will be saved to the configured {@link Context#outputPath}, if set.
     * Otherwise the files will be converted in place in the same directory as the markdown file being converted. Any exceptions
     * thrown will be collected and thrown after processing all documents.
     *
     * @throws AlexandriaException Exception wrapping all exceptions thrown during document processing.
     */
    public void convert() throws AlexandriaException {
        log.debug("Converting files to html.");

        if(context.config().remote().supportsNativeMarkdown()){
            log.debug("Remote supports native markdown, no need to convert anything.");
            return;
        }

        BatchProcess<Config.DocumentMetadata> batchProcess = new BatchProcess<>(context);
        batchProcess.execute(context -> context.config().metadata().get(), (context, metadata) -> {
            log.debug(String.format("Converting %s.", metadata.sourcePath().toFile().getName()));
            AlexandriaConvert.convert(context, metadata);
        }, (context, exceptions) -> {
            log.info(String.format("%d out of %d files converted successfully.",
                    context.documentCount()-exceptions.size(), context.documentCount()));
            Alexandria.save(context);
            return BatchProcess.EXCEPTIONS_UNHANDLED;
        });
    }

    protected static Path convertedPath(Context context, Config.DocumentMetadata metadata){
        Path sourceDir = context.resolveRelativePath(metadata.sourcePath()).getParent();
        String convertedDir = context.outputPath().orElse(sourceDir).toString();
        String convertedFileName = FilenameUtils.getBaseName(metadata.sourcePath().toFile().getName()) + ".html";
        return Paths.get(convertedDir, convertedFileName);
    }

    protected static void convert(Context context, Config.DocumentMetadata metadata) throws AlexandriaException {
        Path convertedPath = convertedPath(context, metadata);
        try {
            Markdown.toHtml(metadata.sourcePath(), convertedPath);
            metadata.convertedChecksum(Optional.of(FileUtils.checksumCRC32(convertedPath.toFile())));
        } catch (IOException e) {
            throw new AlexandriaException.Builder()
                    .withMessage(String.format("Unexcepted error converting %s to html", metadata.sourcePath()))
                    .causedBy(e)
                    .metadataContext(metadata)
                    .build();
        }
        context.convertedPath(metadata, convertedPath);
    }
}
