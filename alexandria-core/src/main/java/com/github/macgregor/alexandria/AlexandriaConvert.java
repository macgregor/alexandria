package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AlexandriaConvert {
    private static Logger log = LoggerFactory.getLogger(AlexandriaConvert.class);

    private Context context;

    public AlexandriaConvert(){}

    public AlexandriaConvert(Context context){
        this.context = context;
    }

    /**
     * Convert HTML files from the files in the metadata index. Converted files will be saved to the configured {@link Context#outputPath}, if set.
     * Otherwise the files will be converted in place in the same directory as the markdown file being converted. Any exceptions
     * thrown will be collected and thrown after processing all documents.
     *
     * @throws AlexandriaException Exception wrapping all exceptions thrown during document processing.
     */
    public void convert() throws AlexandriaException {
        log.debug("Converting files to html.");

        if(supportsNativeMarkdown(context)){
            log.debug("Remote supports native markdown, no need to convert anything.");
            return;
        }

        BatchProcess<Config.DocumentMetadata> batchProcess = new BatchProcess<>(context);
        batchProcess.execute(context -> context.config().metadata().get(), (context, metadata) -> {
            log.debug(String.format("Converting %s.", metadata.sourcePath().toFile().getName()));
            AlexandriaConvert.convert(context, metadata);
        }, (context, exceptions) -> {
            log.info(String.format("%d out of %d files converted successfully.",
                    context.config().metadata().get().size()-exceptions.size(),
                    context.config().metadata().get().size()));
            return false;
        });
    }

    protected static boolean supportsNativeMarkdown(Context context){
        return context.config().remote().supportsNativeMarkdown().isPresent() &&
                context.config().remote().supportsNativeMarkdown().get();
    }

    protected static Path convertedPath(Context context, Config.DocumentMetadata metadata){
        Path sourceDir = metadata.sourcePath().toAbsolutePath().getParent();
        String convertedDir = context.output().orElse(sourceDir.toString());
        String convertedFileName = FilenameUtils.getBaseName(metadata.sourcePath().toFile().getName()) + ".html";
        return Paths.get(convertedDir, convertedFileName);
    }

    protected static void convert(Context context, Config.DocumentMetadata metadata) throws AlexandriaException {
        Path convertedPath = convertedPath(context, metadata);
        try {
            Markdown.toHtml(metadata.sourcePath(), convertedPath);
        } catch (IOException e) {
            throw new AlexandriaException.Builder()
                    .withMessage(String.format("Unexcepted error converting %s to html", metadata.sourcePath()))
                    .causedBy(e)
                    .metadataContext(metadata)
                    .build();
        }
        context.convertedPath(metadata, convertedPath);
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}
