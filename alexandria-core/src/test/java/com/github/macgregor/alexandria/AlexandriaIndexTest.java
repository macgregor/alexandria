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
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AlexandriaIndexTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public TemporaryFolder hierarchy = new TemporaryFolder();

    private File f1;
    private File f2;
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
        f1 = folder.newFile("readme.md");
        f2 = folder.newFile("doc.md");

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
        alexandriaIndex.findUnindexedFiles();
        assertThat(config.metadata()).isPresent();
        assertThat(config.metadata().get()
                .stream()
                .map(m -> m.sourcePath().toFile().getName())
                .collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md", "doc.md");
    }

    @Test
    public void testIndexUpdate() throws IOException, BatchProcessException {
        Config.DocumentMetadata readmeMetadata = new Config.DocumentMetadata();
        readmeMetadata.sourcePath(f1.toPath());
        readmeMetadata.title("Custom Title");
        config.metadata().get().add(readmeMetadata);

        alexandriaIndex.findUnindexedFiles();
        assertThat(config.metadata()).isPresent();
        assertThat(config.metadata().get()
                .stream()
                .map(m -> m.sourcePath().toFile().getName())
                .collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md", "doc.md");
        assertThat(config.metadata().get().get(0).title()).isEqualTo("Custom Title");
    }

    @Test
    public void testIndexSetsTitle() throws IOException, BatchProcessException {
        alexandriaIndex.findUnindexedFiles();
        assertThat(config.metadata()).isPresent();
        assertThat(config.metadata().get()
                .stream()
                .map(m -> m.title())
                .collect(Collectors.toList()))
                .containsExactlyInAnyOrder("readme.md", "doc.md");
    }

    @Test
    public void testIndexSetsSourcePathRelativeToProjectBase() throws IOException, BatchProcessException {
        context.searchPath(Arrays.asList(hierarchy.getRoot().toPath()));
        context.configPath(hierarchy.newFile().toPath());
        context.projectBase(Paths.get(hierarchy.getRoot().toString()));

        alexandriaIndex.findUnindexedFiles();
        assertThat(config.metadata()).isPresent();
        assertThat(config.metadata().get()).hasSize(3);

        Path[] expected = new Path[3];
        expected[0] = Paths.get(hierarchy.getRoot().toString()).relativize(l1Readme.toPath());
        expected[1] = Paths.get(hierarchy.getRoot().toString()).relativize(l2Readme.toPath());
        expected[2] = Paths.get(hierarchy.getRoot().toString()).relativize(l3Readme.toPath());
        assertThat(config.metadata().get().stream()
                .map(m -> m.sourcePath())
                .collect(Collectors.toList()))
                .containsExactlyInAnyOrder(expected);
    }

    @Test
    public void testIndexLoadsRelativeSourcePathsCorrectly() throws IOException, BatchProcessException {
        context.searchPath(Arrays.asList(hierarchy.getRoot().toPath()));
        context.configPath(hierarchy.newFile().toPath());
        context.projectBase(Paths.get(hierarchy.getRoot().toString()));

        alexandriaIndex.findUnindexedFiles();
        Context.save(context);

        Context reloaded = Context.load(context.configPath().toString());
        reloaded.searchPath(Arrays.asList(hierarchy.getRoot().toPath()));
        alexandriaIndex.findUnindexedFiles();
        assertThat(reloaded.config().metadata()).isPresent();
        assertThat(reloaded.config().metadata().get()).hasSize(3);

        Path[] expected = new Path[3];
        expected[0] = Paths.get(hierarchy.getRoot().toString()).relativize(l1Readme.toPath());
        expected[1] = Paths.get(hierarchy.getRoot().toString()).relativize(l2Readme.toPath());
        expected[2] = Paths.get(hierarchy.getRoot().toString()).relativize(l3Readme.toPath());
        assertThat(reloaded.config().metadata().get().stream()
                .map(m -> m.sourcePath())
                .collect(Collectors.toList()))
                .containsExactlyInAnyOrder(expected);
    }

    @Test
    public void testIndexWrapsExceptionsInBatchProcessException(){
        Config config = new Config();
        Context context = new Context();
        context.config(config);

        AlexandriaIndex alexandriaIndex = new AlexandriaIndex();
        alexandriaIndex.context(context);
        assertThatThrownBy(() -> alexandriaIndex.findUnindexedFiles())
                .isInstanceOf(BatchProcessException.class);
    }

    @Test
    public void testIndexDocumentsMatchWrapsException(){
        Context context = new Context();
        context.searchPath(Collections.singletonList(Paths.get("foo")));
        assertThatThrownBy(() -> AlexandriaIndex.documentsMatched(context)).isInstanceOf(AlexandriaException.class);
    }

    @Test
    public void testIndexMarksMissingFilesForDeletion() throws IOException {
        Path deletedPath = Paths.get(folder.getRoot().toString(), "deleted");
        Config.DocumentMetadata deletedDocument = new Config.DocumentMetadata();
        deletedDocument.sourcePath(deletedPath);
        config.metadata(Optional.of(Arrays.asList(deletedDocument)));
        alexandriaIndex.markFilesForDeletion();
        assertThat(config.metadata()).isPresent();
        assertThat(config.metadata().get()
                .stream()
                .filter(d -> d.sourcePath().equals(deletedPath))
                .findFirst()
                .get()
                .extraProps()
                .get()).containsKey("delete");
    }
}
