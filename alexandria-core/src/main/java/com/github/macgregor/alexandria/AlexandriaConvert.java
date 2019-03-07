package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.markdown.MarkdownConverter;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Converts indexed documents from markdown using a {@link MarkdownConverter} which is specified on the
 * {@link com.github.macgregor.alexandria.Config.RemoteConfig}.
 *
 * The default {@link com.github.macgregor.alexandria.markdown.NoopMarkdownConverter} can be used for remotes that support
 * native markdown, though different platforms have slightly different markdown syntax so even there you may need a converter
 * to tweak things.
 *
 * @see MarkdownConverter
 */
@Slf4j
@ToString
@Getter @Setter @Accessors(fluent = true)
@NoArgsConstructor @AllArgsConstructor
public class AlexandriaConvert {

    @NonNull private Context context;
    @NonNull private MarkdownConverter markdownConverter;

    public AlexandriaConvert(Context context) throws AlexandriaException {
        this.context = context;
        this.markdownConverter = context.configureRemote().markdownConverter();
    }

    /**
     * Convert all files in the metadata index using the provided {@link MarkdownConverter}.
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

        BatchProcess<Config.DocumentMetadata> batchProcess = new BatchProcess<>(context);
        batchProcess.execute(context -> context.config().metadata().get(), (context, metadata) -> {
            log.debug(String.format("Converting %s.", metadata.sourceFileName()));

            Config.DocumentMetadata.State state = metadata.determineState();
            if(Config.DocumentMetadata.State.DELETED.equals(state) || Config.DocumentMetadata.State.DELETE.equals(state)){
                log.debug(String.format("Not converting deleted file %s", metadata.sourceFileName()));
                return;
            }
            AlexandriaConvert.convert(context, metadata, markdownConverter);
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
     * Converted file names will have a hash code added to the source file name. This prevents name collisions when outputting
     * converted files to a single directory (such as projects that have submodules with their own README files) or in
     * if converting in place and the converted document has the same type as the source (markdown to markdown).
     *
     * @param context  Alexandria context containing information necessary to calculate the path
     * @param metadata  the particular document being processed
     * @param markdownConverter  Markdown conversion implementation containing the file extension for the converted document
     * @return  absolute path to the converted html file
     */
    protected static Path convertedPath(Context context, Config.DocumentMetadata metadata, MarkdownConverter markdownConverter){
        Path sourceDir =  metadata.sourcePath().getParent();
        String convertedDir = context.outputPath().orElse(sourceDir).toString();
        String convertedFileName = String.format("%s-%s.%s", FilenameUtils.getBaseName(metadata.sourcePath().toFile().getName()),
                sourceDir.toString().hashCode(), markdownConverter.convertedType().getFileExtension());
        Path convertedPath = Paths.get(convertedDir, convertedFileName);
        return convertedPath;
    }

    /**
     * Convert the document from markdown using the {@link MarkdownConverter}.
     *
     * @see MarkdownConverter
     *
     * @param context  Alexandria context containing information necessary to calculate the path
     * @param metadata  the particular document being processed
     * @param markdownConverter  Markdown conversion implementation handling the brunt of the work
     * @throws AlexandriaException  wrapper for any IOException thrown during conversion to make it integrate with {@link BatchProcess}
     */
    protected static void convert(Context context, Config.DocumentMetadata metadata, MarkdownConverter markdownConverter) throws AlexandriaException {
        try {
            Path convertedPath = convertedPath(context, metadata, markdownConverter);
            Path sourcePath = metadata.sourcePath();
            markdownConverter.convert(metadata, sourcePath, convertedPath);
            metadata.convertedChecksum(Optional.of(FileUtils.checksumCRC32(convertedPath.toFile())));
            metadata.convertedPath(Optional.of(convertedPath));
            context.convertedPath(metadata, convertedPath);
        } catch(AlexandriaException e){
            throw e;
        } catch (Exception e) {
            throw new AlexandriaException.Builder()
                    .withMessage(String.format("Unexcepted error converting %s to html", metadata.sourcePath()))
                    .causedBy(e)
                    .metadataContext(metadata)
                    .build();
        }
    }
}
