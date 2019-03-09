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

    public static final String INTERMEDIATE_CONVERTED_PATH_PATTERN = "%s-%s-int.%s";
    public static final String FINAL_CONVERTED_PATH_PATTERN = "%s-%s-fin.%s";
    public static final String FOOTER_SEPARATOR = "\n<p/>\n---\n";
    public static final String DEFAULT_FOOTER = "This file is managed by [Alexandria](https://github.com/macgregor/alexandria), any changes made here will be overwritten.";

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
     * Converted file names will have the hash code of the source directory added added to the source file name as well
     * as "fin" indicating it is the final converted state. This prevents name collisions when outputting converted files
     * to a single directory (such as projects that have submodules with their own README files) or in if converting in
     * place and the converted document has the same type as the source (markdown to markdown).
     *
     * @param context  Alexandria context containing information necessary to calculate the path
     * @param metadata  the particular document being processed
     * @param markdownConverter  Markdown conversion implementation containing the file extension for the converted document
     * @return  absolute path to the final converted file
     */
    protected static Path convertedPath(Context context, Config.DocumentMetadata metadata, MarkdownConverter markdownConverter){
        Path sourceDir =  metadata.sourcePath().getParent();
        String convertedDir = context.outputPath().orElse(sourceDir).toString();
        String convertedFileName = String.format(FINAL_CONVERTED_PATH_PATTERN,
                FilenameUtils.getBaseName(metadata.sourcePath().toFile().getName()),
                sourceDir.toString().hashCode(),
                markdownConverter.convertedType().getFileExtension());
        Path convertedPath = Paths.get(convertedDir, convertedFileName);
        return convertedPath;
    }

    /**
     * Determine what the output path for the intermediate converted files should be.
     *
     * This is nearly identical to {@link AlexandriaConvert#convertedPath(Context, Config.DocumentMetadata, MarkdownConverter)}
     * except that it uses "int" instead of "fin" when determining the filename to use. This intermediate state lets us
     * do pre-processing of the source markdown file before converting, such as adding a footer to all files.
     *
     *
     * @param context  Alexandria context containing information necessary to calculate the path
     * @param metadata  the particular document being processed
     * @return  absolute path to the intermediate converted file
     */
    protected static Path intermediatePath(Context context, Config.DocumentMetadata metadata){
        Path sourceDir =  metadata.sourcePath().getParent();
        String convertedDir = context.outputPath().orElse(sourceDir).toString();
        String intermediateFileName = String.format(INTERMEDIATE_CONVERTED_PATH_PATTERN,
                FilenameUtils.getBaseName(metadata.sourcePath().toFile().getName()),
                sourceDir.toString().hashCode(),
                FilenameUtils.getExtension(metadata.sourcePath().toFile().getName()));
        Path intermediatePath = Paths.get(convertedDir, intermediateFileName);
        return intermediatePath;
    }

    /**
     * Convert the document from markdown using the {@link MarkdownConverter}.
     *
     * @see MarkdownConverter
     *
     * @param context  Alexandria context containing information necessary to calculate the converted paths
     * @param metadata  the particular document being processed
     * @param markdownConverter  Markdown conversion implementation handling the brunt of the work
     * @throws AlexandriaException  wrapper for any IOException thrown during conversion to make it integrate with {@link BatchProcess}
     */
    protected static void convert(Context context, Config.DocumentMetadata metadata, MarkdownConverter markdownConverter) throws AlexandriaException {
        try {
            Path convertedPath = convertedPath(context, metadata, markdownConverter);
            Path sourcePath = metadata.sourcePath();
            if(context.disclaimerFooterEnabled()){
                AlexandriaConvert.addDisclaimer(context, metadata);
                sourcePath = AlexandriaConvert.intermediatePath(context, metadata);
            }
            markdownConverter.convert(metadata, sourcePath, convertedPath);
            metadata.convertedChecksum(Optional.of(FileUtils.checksumCRC32(convertedPath.toFile())));
            metadata.convertedPath(Optional.of(convertedPath));
            context.convertedPath(metadata, convertedPath);
        } catch(AlexandriaException e){
            throw e;
        } catch (Exception e) {
            throw new AlexandriaException.Builder()
                    .withMessage(String.format("Unexcepted error converting %s to html.", metadata.sourcePath()))
                    .causedBy(e)
                    .metadataContext(metadata)
                    .build();
        }
    }

    /**
     * Adds a footer to the bottom of each markdown document before the final conversion to warn readers that they
     * are not viewing the source document and changes will be overwritten.
     *
     * A new line and separator ({@code ---}) will be added between the document and the footer. A custom footer can be
     * used by specifying it in {@link Context#disclaimerFooterPath}, the file should be a markdown file. If no custom
     * path is specified, a static default is provided.
     *
     * @param context  Alexandria context containing information necessary to calculate the converted paths
     * @param metadata  the particular document being processed
     * @throws AlexandriaException  wrapper for any IOException thrown during conversion to make it integrate with {@link BatchProcess}
     */
    protected static void addDisclaimer(Context context, Config.DocumentMetadata metadata) throws AlexandriaException {
        try {
            String footer = context.disclaimerFooterPath.isPresent() ?
                    Resources.load(context.disclaimerFooterPath.get().toString()) : DEFAULT_FOOTER;
            Path intermediatePath = AlexandriaConvert.intermediatePath(context, metadata);
            metadata.intermediateConvertedPath(Optional.of(intermediatePath));
            Path sourcePath = metadata.sourcePath();
            String merged = Resources.load(sourcePath.toString()) + AlexandriaConvert.FOOTER_SEPARATOR + footer;
            Resources.save(intermediatePath.toString(), merged, true);
        } catch(IOException e){
            throw new AlexandriaException.Builder()
                    .withMessage(String.format("Unexcepted error adding disclaimer footer to %s.", metadata.sourcePath()))
                    .causedBy(e)
                    .metadataContext(metadata)
                    .build();
        }
    }
}
