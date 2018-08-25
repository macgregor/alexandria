package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.exceptions.BatchProcessException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AlexandriaConvertTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testConvertDoesntConvertWhenRemoteSupportsMarkdown() throws IOException, BatchProcessException {
        File f1 = folder.newFile("readme.md");

        Config config = new Config();
        Config.RemoteConfig remoteConfig = new Config.RemoteConfig();
        remoteConfig.supportsNativeMarkdown(Optional.of(true));
        config.remote(remoteConfig);

        Context context = new Context();
        context.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        context.config(config);

        Alexandria alexandria = new Alexandria();
        alexandria.context(context);
        alexandria.convert();
        assertThat(Paths.get(folder.getRoot().toString(), "readme.html")).doesNotExist();
    }

    @Test
    public void testConvertSetsConvertedPathPreferringConfigOverride() throws IOException, BatchProcessException {
        File f1 = folder.newFile("readme.md");
        File subdir = folder.newFolder("out");

        Config config = new Config();
        Config.DocumentMetadata readmeMetadata = new Config.DocumentMetadata();
        readmeMetadata.sourcePath(f1.toPath());
        config.metadata().get().add(readmeMetadata);

        Context context = new Context();
        context.outputPath(Optional.of(subdir.getPath()));
        context.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        context.config(config);

        Alexandria alexandria = new Alexandria();
        alexandria.context(context);
        alexandria.convert();
        assertThat(context.convertedPath(readmeMetadata).get()).isEqualTo(Paths.get(subdir.getPath(), "readme.html"));
        assertThat(Paths.get(subdir.getPath(), "readme.html")).exists();
    }

    @Test
    public void testConvertSetsConvertedPathUsesSourceDirAsOutput() throws IOException, BatchProcessException {
        File f1 = folder.newFile("readme.md");

        Config config = new Config();
        Config.DocumentMetadata readmeMetadata = new Config.DocumentMetadata();
        readmeMetadata.sourcePath(f1.toPath());
        config.metadata().get().add(readmeMetadata);

        Context context = new Context();
        context.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        context.config(config);

        Alexandria alexandria = new Alexandria();
        alexandria.context(context);
        alexandria.convert();
        assertThat(context.convertedPath(readmeMetadata).get()).isEqualTo(Paths.get(folder.getRoot().toString(), "readme.html"));
        assertThat(Paths.get(folder.getRoot().toString(), "readme.html")).exists();
    }

    @Test
    public void testConvertWrapsConvertErrorsPerDocument() {
        Config config = new Config();
        Config.DocumentMetadata readmeMetadata = new Config.DocumentMetadata();
        config.metadata().get().add(readmeMetadata);

        Context context = new Context();
        context.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        context.config(config);

        Alexandria alexandria = new Alexandria();
        alexandria.context(context);
        assertThatThrownBy(() -> alexandria.convert()).isInstanceOf(BatchProcessException.class);
    }

    @Test
    public void testSupportNativeMarkdownFalseWhenNoRemoteHasntSetField(){
        Context context = new Context();
        context.config(new Config());
        context.config().remote(new Config.RemoteConfig());
        context.config().remote().supportsNativeMarkdown(Optional.empty());
        assertThat(AlexandriaConvert.supportsNativeMarkdown(context)).isFalse();
    }

    @Test
    public void testConvertWrapsIOExceptionAsAlexandriaException(){
        Context context = new Context();
        context.config(new Config());
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        assertThatThrownBy(() -> AlexandriaConvert.convert(context, metadata)).isInstanceOf(AlexandriaException.class);
    }
}
