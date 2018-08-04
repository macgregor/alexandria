package com.github.macgregor.alexandria;

import picocli.CommandLine;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@CommandLine.Command(description = "Converts markdown files into html files.",
        name = "convert", mixinStandardHelpOptions = true)
public class ConvertCommand implements Callable<Void> {

    @CommandLine.Option(names = { "-o", "--output" }, description = "Output directory for converted files. If not specified, will convert file in place.")
    private String output;

    @CommandLine.Option(names = { "-w", "--overwrite" }, description = "Overwrite files that exist in output directory. Default ${DEFAULT-VALUE}.")
    private boolean overwrite = true;

    @CommandLine.Parameters(arity = "1..*", paramLabel = "INPUT_DIRECTORIES", description = "One or more directories to search for files in.")
    private List<String> input;

    @Override
    public Void call() throws Exception {
        MarkdownConverter converter = new MarkdownConverter(input, Optional.ofNullable(output), Optional.of(overwrite));
        List<Metadata> converted = converter.convert();
        for(Metadata m : converted){
            System.out.println(String.format("Created %s", m.getConverted().get().toString()));
        }
        return null;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public List<String> getInput() {
        return input;
    }

    public void setInput(List<String> input) {
        this.input = input;
    }
}
