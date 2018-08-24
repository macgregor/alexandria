package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.exceptions.BatchProcessException;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AlexandriaConvert {
    private static Logger log = LoggerFactory.getLogger(AlexandriaConvert.class);

    private Context context;
    private List<AlexandriaException> exceptions = new ArrayList<>();

    public AlexandriaConvert(){}

    public AlexandriaConvert(Context context){
        this.context = context;
    }

    /**
     * Convert HTML files from the files in the metadata index. Converted files will be saved to the configured {@link Context#outputPath}, if set.
     * Otherwise the files will be converted in place in the same directory as the markdown file being converted. Any exceptions
     * thrown will be collected and thrown after processing all documents.
     *
     * @throws BatchProcessException Exception wrapping all exceptions thrown during document processing.
     */
    public void convert() throws BatchProcessException {
        log.debug("Converting files to html.");

        if(supportsNativeMarkdown(context)){
            log.debug("Remote supports native markdown, no need to convert anything.");
            return;
        }

        for(Config.DocumentMetadata metadata : context.config().metadata().get()){
            try {
                log.debug(String.format("Converting %s.", metadata.sourcePath().toFile().getName()));

                context.convertedPath(metadata, convertedPath(context, metadata));
                Markdown.toHtml(metadata.sourcePath(), context.convertedPath(metadata).get());
            } catch(Exception e){
                log.warn(String.format("Unexcepted error converting %s to html", metadata.sourcePath()), e);

                exceptions.add(new AlexandriaException.Builder()
                        .withMessage(String.format("Unexcepted error converting %s to html", metadata.sourcePath()))
                        .causedBy(e)
                        .metadataContext(metadata)
                        .build());
            }
        }
        log.info(String.format("%d out of %d files converted successfully.", context.config().metadata().get().size()-exceptions.size(), context.config().metadata().get().size()));

        if(exceptions.size() > 0){
            throw new BatchProcessException.Builder()
                    .withMessage(String.format("Failed to convert %d out of %d documents to html", exceptions.size(), context.config().metadata().get().size()))
                    .causedBy(exceptions)
                    .build();
        }
    }

    private boolean supportsNativeMarkdown(Context context){
        return context.config().remote().supportsNativeMarkdown().isPresent() &&
                context.config().remote().supportsNativeMarkdown().get();
    }

    private Path convertedPath(Context context, Config.DocumentMetadata metadata){
        Path sourceDir = metadata.sourcePath().toAbsolutePath().getParent();
        String convertedDir = context.output().orElse(sourceDir.toString());
        String convertedFileName = FilenameUtils.getBaseName(metadata.sourcePath().toFile().getName()) + ".html";
        return Paths.get(convertedDir, convertedFileName);
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public List<AlexandriaException> getExceptions() {
        return exceptions;
    }

    public void setExceptions(List<AlexandriaException> exceptions) {
        this.exceptions = exceptions;
    }
}
