import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(description = "Converts .",
        name = "checksum", mixinStandardHelpOptions = true, version = "checksum 3.0")
public class Application {

    @CommandLine.Option(names = { "-o", "--output" }, description = "Output directory for converted files.")
    private boolean output = false;

    @CommandLine.Parameters(arity = "1..*", paramLabel = "FILE", description = "File(s) to process.")
    private File[] inputFiles;

    public static void main(String[] args){

    }
}
