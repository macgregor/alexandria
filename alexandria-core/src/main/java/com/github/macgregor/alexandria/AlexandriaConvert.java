package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.markdown.MarkdownConverter;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
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
     * Determine if the document needs to be converted. Doesnt actually do the conversion or update {@link Context}.
     *
     * Get the converted file path from {@link Context#convertedPath(Config.DocumentMetadata)} or calculate what it
     * should be if the path isnt present. Once we have a path, we calculate the current checksum of the converted document
     * and compare it to {@link com.github.macgregor.alexandria.Config.DocumentMetadata#convertedChecksum} and only convert
     * if it is different.
     *
     * @see AlexandriaConvert#convert(Context, Config.DocumentMetadata, MarkdownConverter)
     *
     * @param context  Alexandria context containing the converted path cache
     * @param metadata  Indexed document being synced
     * @param markdownConverter  Markdown conversion implementation needed by
     *      {@link AlexandriaConvert#convertedPath(Context, Config.DocumentMetadata, MarkdownConverter)}
     * @return  False if the converted file path exists and is up to date, otherwise true.
     * @throws IOException  Error calculating checksum
     */
    protected static boolean needsConversion(Context context, Config.DocumentMetadata metadata, MarkdownConverter markdownConverter) throws IOException {
        //look for converted file in the context cache or recalculate the path if its not there
        Path convertedPathGuess = context.convertedPath(metadata)
                .orElse(AlexandriaConvert.convertedPath(context, metadata, markdownConverter));

        //if the file exists, check if it matches the stored checksum
        if(convertedPathGuess.toFile().exists()){
            long currentChecksum = FileUtils.checksumCRC32(convertedPathGuess.toFile());
            if(metadata.convertedChecksum().isPresent() && metadata.convertedChecksum().get().equals(currentChecksum)){
                context.convertedPath(metadata, convertedPathGuess);
                return false;
            }
        }

        //remote needs conversion and the file doesnt exist
        return true;
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

    /**
     * Converts an indexed document if needed.
     *
     * When Alexandria is run with its full lifecycle (index, convert, sync) this should be a noop, but if a user is running
     * individual phases we could find ourselves in a position where the files havent been converted yet, or they have been
     * converted but the file path isnt in the {@link Context#convertedPaths} cache. Instead of throwing an error, we
     * simply reconvert the file on the fly and add it to the context.
     *
     * If the remote supports native markdown, this is a noop.
     *
     * @see AlexandriaConvert#convert(Context, Config.DocumentMetadata, MarkdownConverter markdownConverter)
     *
     * @param context  Alexandria context containing the converted path cache
     * @param metadata  Indexed document being synced
     * @param markdownConverter  Markdown conversion implementation handling the brunt of the work
     * @throws IOException  Errors calculating checksums or general file IO problems
     */
    protected static void convertAsNeeded(Context context, Config.DocumentMetadata metadata, MarkdownConverter markdownConverter) throws IOException {
        if(needsConversion(context, metadata, markdownConverter)) {
            AlexandriaConvert.convert(context, metadata, markdownConverter);
        }
    }
}
