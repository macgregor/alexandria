package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.exceptions.BatchProcessException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

public class AlexandriaIndexTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public TemporaryFolder hierarchy = new TemporaryFolder();

    private File l1Readme;
    private File l2Dir;
    private File l2Readme;
    private File l3Dir;
    private File l3Readme;

    private Context context;
    private Config config;
    private AlexandriaIndex alexandriaIndex;

    @Before
    public void setup() throws IOException {

        l1Readme = hierarchy.newFile("readme.md");
        l2Dir = hierarchy.newFolder("l2");
        l2Readme = new File(l2Dir, "readme.md");
        l2Readme.createNewFile();
        l3Dir = new File(l2Dir, "l3");
        l3Dir.mkdir();
        l3Readme = new File(l3Dir, "readme.md");
        l3Readme.createNewFile();

        config = new Config();
        context = new Context();
        context.searchPath(Arrays.asList(folder.getRoot().toPath()));
        context.configPath(folder.newFile().toPath());
        context.projectBase(folder.getRoot().toPath());
        context.config(config);

        alexandriaIndex = new AlexandriaIndex(context);
    }

    @Test
    public void testIndexFresh() throws IOException, BatchProcessException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = context.config().metadata().get().get(0);
        context.config().metadata(Optional.of(new ArrayList<>()));
        AlexandriaIndex alexandriaIndex = new AlexandriaIndex(context);
        alexandriaIndex.update();
        assertThat(context.config().metadata()).isPresent();
        assertThat(context.config().metadata().get()).hasSize(1);
        assertThat(context.config().metadata().get().get(0)).isEqualTo(metadata);
    }

    @Test
    public void testIndexUpdate() throws IOException, BatchProcessException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata alreadyIndexed = context.config().metadata().get().get(0);
        alreadyIndexed.title("Custom Title");
        Config.DocumentMetadata newDocument = TestData.minimalDocumentMetadata(folder);
        AlexandriaIndex alexandriaIndex = new AlexandriaIndex(context);

        alexandriaIndex.update();
        assertThat(context.config().metadata().get()
                .stream()
                .map(m -> m.sourcePath().toFile().getName())
                .collect(Collectors.toList()))
                .containsExactlyInAnyOrder(alreadyIndexed.sourcePath().toFile().getName(), newDocument.sourcePath().toFile().getName());
        assertThat(alreadyIndexed.title()).isEqualTo("Custom Title");
    }

    @Test
    public void testIndexSetsTitle() throws IOException, BatchProcessException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata alreadyIndexed = context.config().metadata().get().get(0);
        context.config().metadata(Optional.of(new ArrayList<>()));
        AlexandriaIndex alexandriaIndex = new AlexandriaIndex(context);
        alexandriaIndex.update();
        assertThat(context.config().metadata()).isPresent();
        assertThat(context.config().metadata().get()
                .stream()
                .map(m -> m.title())
                .collect(Collectors.toList()))
                .containsExactlyInAnyOrder(alreadyIndexed.title());
    }

    @Test
    public void testIndexSetsSourcePathRelativeToProjectBase() throws IOException, BatchProcessException {
        context.searchPath(Arrays.asList(hierarchy.getRoot().toPath()));
        context.configPath(hierarchy.newFile().toPath());
        context.projectBase(Paths.get(hierarchy.getRoot().toString()));

        alexandriaIndex.update();
        assertThat(config.metadata()).isPresent();
        assertThat(config.metadata().get()).hasSize(3);

        Path[] expected = new Path[3];
        expected[0] = l1Readme.toPath();
        expected[1] = l2Readme.toPath();
        expected[2] = l3Readme.toPath();
        assertThat(config.metadata().get().stream()
                .map(m -> m.sourcePath())
                .collect(Collectors.toList()))
                .containsExactlyInAnyOrder(expected);
    }

    @Test
    public void testIndexWrapsExceptionsInBatchProcessException() throws IOException {
        Context context = spy(TestData.minimalContext(folder));
        context.config().metadata(Optional.empty());
        doThrow(new RuntimeException("test exception")).when(context).addMetadata(any(Config.DocumentMetadata.class));
        AlexandriaIndex alexandriaIndex = new AlexandriaIndex(context);
        assertThatThrownBy(() -> alexandriaIndex.update())
                .isInstanceOf(BatchProcessException.class);
    }

    @Test
    public void testIndexDocumentsMatchWrapsException(){
        Context context = new Context();
        context.searchPath(Collections.singletonList(Paths.get("foo")));
        assertThatThrownBy(() -> AlexandriaIndex.documentsMatched(context)).isInstanceOf(AlexandriaException.class);
    }
}
