package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.exceptions.BatchProcessException;
import org.apache.commons.io.FileUtils;
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
    public void testConvertDoesntConvertWhenRemoteSupportsMarkdown() throws IOException {
        Context context = TestData.minimalContext(folder);
        context.config().remote().supportsNativeMarkdown(true);

        AlexandriaConvert alexandriaConvert = new AlexandriaConvert(context);
        alexandriaConvert.convert();
        assertThat(Paths.get(folder.getRoot().toString(), "readme.html")).doesNotExist();
    }

    @Test
    public void testConvertSetsConvertedPathPreferringConfigOverride() throws IOException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);

        File subdir = folder.newFolder("out");
        context.outputPath(Optional.of(subdir.toPath()));

        AlexandriaConvert alexandriaConvert = new AlexandriaConvert(context);
        alexandriaConvert.convert();
        assertThat(context.convertedPath(metadata).get()).isEqualTo(Paths.get(subdir.getPath(), "minimal.html"));
        assertThat(Paths.get(subdir.getPath(), "minimal.html")).exists();
    }

    @Test
    public void testConvertSetsConvertedPathUsesSourceDirAsOutput() throws IOException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        AlexandriaConvert alexandriaConvert = new AlexandriaConvert(context);
        alexandriaConvert.convert();
        assertThat(context.convertedPath(metadata).get()).isEqualTo(Paths.get(folder.getRoot().toString(), "minimal.html"));
        assertThat(Paths.get(folder.getRoot().toString(), "minimal.html")).exists();
    }

    @Test
    public void testConvertWrapsConvertErrorsPerDocument() throws IOException {
        Context context = TestData.minimalContext(folder);
        FileUtils.forceDelete(context.config().metadata().get().get(0).sourcePath().toFile());
        Alexandria alexandria = new Alexandria();
        alexandria.context(context);
        assertThatThrownBy(() -> alexandria.convert()).isInstanceOf(BatchProcessException.class);
    }

    @Test
    public void testConvertWrapsIOExceptionAsAlexandriaException() throws IOException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        metadata.sourcePath(Paths.get("foo"));
        assertThatThrownBy(() -> AlexandriaConvert.convert(context, metadata)).isInstanceOf(AlexandriaException.class);
    }

    @Test
    public void testConvertCalculatesChecksumOfConvertedFile() throws IOException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        AlexandriaConvert alexandriaConvert = new AlexandriaConvert(context);
        alexandriaConvert.convert();
        assertThat(metadata.convertedChecksum()).isPresent();
        assertThat(metadata.convertedChecksum().get()).isEqualTo(FileUtils.checksumCRC32(context.convertedPath(metadata).get().toFile()));
    }
}
