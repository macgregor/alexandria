package com.github.macgregor.alexandria;

import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MarkdownConverter {

    private Set<Path> documentDirectories;
    private Optional<Path> outputDir;
    private Optional<Boolean> overwriteFiles;

    public MarkdownConverter(Collection<String> documentDirectories, String outputDir, Optional<Boolean> overwriteFiles) throws FileNotFoundException {
        this.documentDirectories = Resources.directoryPaths(documentDirectories, true);
        this.overwriteFiles = overwriteFiles;
        this.outputDir = Optional.of(Resources.path(outputDir));
    }

    public MarkdownConverter(Collection<String> documentDirectories, String outputDir) throws FileNotFoundException {
        this.documentDirectories = Resources.directoryPaths(documentDirectories, true);
        this.outputDir = Optional.of(Resources.path(outputDir));
        this.overwriteFiles = Optional.of(true);
    }

    public MarkdownConverter(Collection<String> documentDirectories) throws FileNotFoundException {
        this.documentDirectories = Resources.directoryPaths(documentDirectories, true);
        this.outputDir = Optional.empty();
        this.overwriteFiles = Optional.of(true);
    }

    public List<Path> convert() throws IOException {
        List<Path> convertedFiles = new ArrayList<>();

        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));

        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        for(Path inputDir : documentDirectories){
            Path output = outputDir.orElse(inputDir);
            for(File in : Resources.files(inputDir, Resources.MATCH_MD_FILES)){
                Node document = parser.parseReader(new FileReader(in));
                String outFileName = in.getName().substring(0, in.getName().lastIndexOf('.')) + ".html";
                Path outputFile = Paths.get(output.toString(), outFileName);
                Resources.save(outputFile, renderer.render(document), overwriteFiles.get());
                convertedFiles.add(outputFile);
            }
        }

        return convertedFiles;
    }

    public Set<Path> getDocumentDirectories() {
        return documentDirectories;
    }

    public void setDocumentDirectories(Set<Path> documentDirectories) {
        this.documentDirectories = documentDirectories;
    }

    public Optional<Path> getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(Optional<Path> outputDir) {
        this.outputDir = outputDir;
    }
}
