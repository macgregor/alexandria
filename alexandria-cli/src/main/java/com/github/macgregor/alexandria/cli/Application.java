package com.github.macgregor.alexandria.cli;

import picocli.CommandLine;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

@CommandLine.Command(
        description = "Command line interface for converting markdown files into html and publish them. Running without a subcommand with trigger all three phases to run in order: index, convert, sync.",
        name = "alexandria",
        mixinStandardHelpOptions = true,
        versionProvider = Application.ManifestVersionProvider.class,
        subcommands = {
            ConvertCommand.class, IndexCommand.class, SyncCommand.class
        })
public class Application extends AlexandriaCommand {

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new Application());
        cmd.setToggleBooleanFlags(false);

        cmd.parseWithHandler(new CommandLine.RunLast(), System.err, args);
    }

    @Override
    public Void call() throws Exception {
        configureLogging();
        init();
        logContext();
        alexandria().index()
                .convert()
                .syncWithRemote();
        return null;
    }

    static class ManifestVersionProvider implements CommandLine.IVersionProvider {
        public String[] getVersion() throws Exception {
            Enumeration<URL> resources = CommandLine.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try {
                    Manifest manifest = new Manifest(url.openStream());
                    if (isApplicableManifest(manifest)) {
                        Attributes attributes = manifest.getMainAttributes();
                        return new String[] { attributes.get(key("Implementation-Title")) + " version \"" + attributes.get(key("Implementation-Version")) + "\"" };
                    }
                } catch (IOException ex) {
                    return new String[] { "Unable to read from " + url + ": " + ex };
                }
            }
            return new String[0];
        }

        private boolean isApplicableManifest(Manifest manifest) {
            Attributes attributes = manifest.getMainAttributes();
            return "alexandria-cli".equals(attributes.get(key("Implementation-Title")));
        }
        private static Attributes.Name key(String key) { return new Attributes.Name(key); }
    }
}
