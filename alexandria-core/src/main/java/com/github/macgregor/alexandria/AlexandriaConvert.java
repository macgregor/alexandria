package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Converts indexed documents from markdown to html.
 *
 * Most of the work is dome by <a href="https://github.com/vsch/flexmark-java">Flexmark</a>, with most
 * of our work managing paths, files and errors.
 *
 * If a remote supports native markdown, the documents will not be converted and this phase becomes essentially a noop.
 *
 * @see Markdown
 */
@Slf4j
@ToString
@Getter @Setter @Accessors(fluent = true)
@NoArgsConstructor @AllArgsConstructor
public class AlexandriaConvert {

    @NonNull private Context context;

    /**
     * Convert all files in the metadata index into html.
     *
     * Converted files will be saved to the configured {@link Context#outputPath}, if set. Otherwise the files will be
     * converted in place in the same directory as the markdown file being converted. Files that have been deleted or
     * marked for deletion will be ignored.
     *
     * @see BatchProcess
     *
     * @throws AlexandriaException  Exception wrapping all exceptions thrown during document processing.
     */
    public void convert() throws AlexandriaException {
        log.debug("Converting files to html.");

        context.makePathsAbsolute();

        if(context.config().remote().supportsNativeMarkdown()){
            log.debug("Remote supports native markdown, no need to convert anything.");
            return;
        }

        BatchProcess<Config.DocumentMetadata> batchProcess = new BatchProcess<>(context);
        batchProcess.execute(context -> context.config().metadata().get(), (context, metadata) -> {
            log.debug(String.format("Converting %s.", metadata.sourceFileName()));

            Config.DocumentMetadata.State state = metadata.determineState();
            if(Config.DocumentMetadata.State.DELETED.equals(state) || Config.DocumentMetadata.State.DELETE.equals(state)){
                log.debug(String.format("Not converting deleted file %s", metadata.sourceFileName()));
                return;
            }
            AlexandriaConvert.convert(context, metadata);
        }, (context, exceptions) -> {
            log.info(String.format("%d out of %d files converted successfully.",
                    context.documentCount()-exceptions.size(), context.documentCount()));
            Context.save(context);
            return BatchProcess.EXCEPTIONS_UNHANDLED;
        });
    }

    /**
     * Determine what the output path for converted files should be.
     *
     * @param context  Alexandria context containing information necessary to calculate the path
     * @param metadata  the particular document being processed
     * @return  absolute path to the converted html file
     */
    protected static Path convertedPath(Context context, Config.DocumentMetadata metadata){
        Path sourceDir =  metadata.sourcePath().getParent();
        String convertedDir = context.outputPath().orElse(sourceDir).toString();
        String convertedFileName = String.format("%s-%s.html", FilenameUtils.getBaseName(metadata.sourcePath().toFile().getName()), sourceDir.toString().hashCode());
        Path convertedPath = Paths.get(convertedDir, convertedFileName);
        return convertedPath;
    }

    /**
     * Convert the document from markdown to html.
     *
     * @see Markdown
     *
     * @param context  Alexandria context containing information necessary to calculate the path
     * @param metadata  the particular document being processed
     * @throws AlexandriaException  wrapper for any IOException thrown during conversion to make it integrate with {@link BatchProcess}
     */
    protected static void convert(Context context, Config.DocumentMetadata metadata) throws AlexandriaException {
        try {
            Path convertedPath = convertedPath(context, metadata);
            Path sourcePath = metadata.sourcePath();
            Markdown.toHtml(context, sourcePath, convertedPath);
            metadata.convertedChecksum(Optional.of(FileUtils.checksumCRC32(convertedPath.toFile())));
            context.convertedPath(metadata, convertedPath);
        } catch (Exception e) {
            throw new AlexandriaException.Builder()
                    .withMessage(String.format("Unexcepted error converting %s to html", metadata.sourcePath()))
                    .causedBy(e)
                    .metadataContext(metadata)
                    .build();
        }
    }
}
