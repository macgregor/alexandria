package com.github.macgregor.alexandria;

import org.apache.commons.io.filefilter.IOFileFilter;
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
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class PathFinderTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    public List<Path> paths;

    @Before
    public void setup() throws IOException {
        paths = setupTestFiles();
    }

    @Test
    public void testPathFinderFindsAllFilesByDefault() throws IOException {
        PathFinder pathFinder = new PathFinder()
                .startingIn(projectBase().toString());
        testPathFinder(pathFinder,
                "readme-a.md", "readme-a.txt", "readme-b.md", "readme-b.txt",
                "ignore-a.md", "ignore-a.txt", "ignore-b.md", "ignore-b.txt");
    }

    @Test
    public void testPathFinderFindsAllFilesObeysRecursionFlag() throws IOException {
        PathFinder pathFinder = new PathFinder()
                .startingIn(Paths.get(moduleABase().toString(), "include").toString())
                .recursive(false)
                .including("*");
        testPathFinder(pathFinder, "readme-a.md");
    }

    @Test
    public void testPathFinderIncludeAndExcludeExactFilename() throws IOException {
        PathFinder pathFinder = new PathFinder()
                .startingIn(projectBase().toString())
                .including(Arrays.asList("readme-a.md", "readme-a.txt", "readme-b.md", "readme-b.txt"))
                .excluding(Arrays.asList("ignore-a.md", "ignore-a.txt", "ignore-b.md", "ignore-b.txt"));
        testPathFinder(pathFinder, "readme-a.md", "readme-a.txt", "readme-b.md", "readme-b.txt");
    }

    @Test
    public void testPathFinderIncludeAndExcludeGlobPattern() throws IOException {
        PathFinder pathFinder = new PathFinder()
                .startingIn(projectBase().toString())
                .including("**/include/**")
                .excluding("**/exclude/**");
        testPathFinder(pathFinder, "readme-a.md", "readme-a.txt", "readme-b.md", "readme-b.txt");
    }

    @Test
    public void testPathFinderIncludeAndExcludeGlobPatternWithPrefix() throws IOException {
        PathFinder pathFinder = new PathFinder()
                .startingIn(projectBase().toString())
                .including("glob:**/include/**")
                .excluding("glob:**/exclude/**");
        testPathFinder(pathFinder, "readme-a.md", "readme-a.txt", "readme-b.md", "readme-b.txt");
    }

    @Test
    public void testPathFinderIncludeAndExcludeGlobPatternRelative() throws IOException {
        PathFinder pathFinder = new PathFinder()
                .startingIn(projectBase().toString())
                .including("./**/include/**")
                .excluding("./**/exclude/**");
        testPathFinder(pathFinder, "readme-a.md", "readme-a.txt", "readme-b.md", "readme-b.txt");
    }

    @Test
    public void testPathFinderIncludeAndExcludeGlobPatternRelativeWithPrefix() throws IOException {
        PathFinder pathFinder = new PathFinder()
                .startingIn(projectBase().toString())
                .including("glob:./**/include/**")
                .excluding("glob:./**/exclude/**");
        testPathFinder(pathFinder, "readme-a.md", "readme-a.txt", "readme-b.md", "readme-b.txt");
    }

    @Test
    public void testPathFinderIncludeMatchesExactFilename() throws IOException {
        PathFinder pathFinder = new PathFinder()
                .startingIn(projectBase().toString())
                .including(Arrays.asList("readme-a.md", "readme-a.txt"));
        testPathFinder(pathFinder, "readme-a.md", "readme-a.txt");
    }

    @Test
    public void testPathFinderIncludeMatchesWildcardFilename() throws IOException {
        PathFinder pathFinder = new PathFinder()
                .startingIn(projectBase().toString())
                .including(Arrays.asList("*-a.md", "*-a.txt"));
        testPathFinder(pathFinder, "readme-a.md", "readme-a.txt", "ignore-a.txt", "ignore-a.md");
    }

    @Test
    public void testPathFinderIncludeMatchesFullPathGlobPattern() throws IOException {
        PathFinder pathFinder = new PathFinder()
                .startingIn(projectBase().toString())
                .including("**/include/**");
        testPathFinder(pathFinder, "readme-a.md", "readme-a.txt", "readme-b.md", "readme-b.txt");
    }

    @Test
    public void testPathFinderIncludeGlobPatternWithPrefix() throws IOException {
        PathFinder pathFinder = new PathFinder()
                .startingIn(projectBase().toString())
                .including("glob:**/include/**");
        testPathFinder(pathFinder, "readme-a.md", "readme-a.txt", "readme-b.md", "readme-b.txt");
    }

    @Test
    public void testPathFinderIncludeGlobPatternRelative() throws IOException {
        PathFinder pathFinder = new PathFinder()
                .startingIn(projectBase().toString())
                .including("./**/include/**");
        testPathFinder(pathFinder, "readme-a.md", "readme-a.txt", "readme-b.md", "readme-b.txt");
    }

    @Test
    public void testPathFinderIncludeGlobPatternRelativeWithPrefix() throws IOException {
        PathFinder pathFinder = new PathFinder()
                .startingIn(projectBase().toString())
                .including("glob:./**/include/**");
        testPathFinder(pathFinder, "readme-a.md", "readme-a.txt", "readme-b.md", "readme-b.txt");
    }

    @Test
    public void testPathFinderExcludeMatchesExactFilename() throws IOException {
        PathFinder pathFinder = new PathFinder()
                .startingIn(projectBase().toString())
                .excluding(Arrays.asList("ignore-a.md", "ignore-a.txt", "ignore-b.md", "ignore-b.txt"));
        testPathFinder(pathFinder, "readme-a.md", "readme-a.txt", "readme-b.md", "readme-b.txt");
    }

    @Test
    public void testPathFinderExcludeMatchesWildcardFilename() throws IOException {
        PathFinder pathFinder = new PathFinder()
                .startingIn(projectBase().toString())
                .excluding(Arrays.asList("ignore*.md", "ignore*.txt"));
        testPathFinder(pathFinder, "readme-a.md", "readme-a.txt", "readme-b.md", "readme-b.txt");
    }

    @Test
    public void testPathFinderExcludeGlobPattern() throws IOException {
        PathFinder pathFinder = new PathFinder()
                .startingIn(projectBase().toString())
                .excluding("**/exclude/**");
        testPathFinder(pathFinder, "readme-a.md", "readme-a.txt", "readme-b.md", "readme-b.txt");
    }

    @Test
    public void testPathFinderExcludeGlobPatternWithPrefix() throws IOException {
        PathFinder pathFinder = new PathFinder()
                .startingIn(projectBase().toString())
                .excluding("glob:**/exclude/**");
        testPathFinder(pathFinder, "readme-a.md", "readme-a.txt", "readme-b.md", "readme-b.txt");
    }

    @Test
    public void testPathFinderExcludeGlobPatternRelative() throws IOException {
        PathFinder pathFinder = new PathFinder()
                .startingIn(projectBase().toString())
                .excluding("./**/exclude/**");
        testPathFinder(pathFinder, "readme-a.md", "readme-a.txt", "readme-b.md", "readme-b.txt");
    }

    @Test
    public void testPathFinderExcludeGlobPatternRelativeWithPrefix() throws IOException {
        PathFinder pathFinder = new PathFinder()
                .startingIn(projectBase().toString())
                .excluding("glob:./**/exclude/**");
        testPathFinder(pathFinder, "readme-a.md", "readme-a.txt", "readme-b.md", "readme-b.txt");
    }

    @Test
    public void testPathFinderChecksStartingDirExists() throws IOException {
        PathFinder pathFinder = new PathFinder();
        assertThatExceptionOfType(IOException.class)
                .isThrownBy(() -> pathFinder.startingIn("nope"))
                .withMessageContaining("Directory nope doesnt exist.");
    }

    @Test
    public void testPathFinderChecksStartingDirIsDirectory() throws IOException {
        File file = folder.newFile();
        PathFinder pathFinder = new PathFinder();
        assertThatExceptionOfType(IOException.class)
                .isThrownBy(() -> pathFinder.startingIn(file.toString()))
                .withMessageContaining("is not a directory.");
    }

    @Test
    public void testPathFinderWithMultipleInputSources() throws IOException {
        List<Path> startingIn = Arrays.asList(moduleABase(), moduleBBase());
        List<String> startingInStrings = startingIn.stream()
                .map(p -> p.toString())
                .collect(Collectors.toList());
        PathFinder pathFinder = new PathFinder().startingIn(startingInStrings);
        testPathFinder(pathFinder, "readme-a.md", "readme-a.txt", "readme-b.md", "readme-b.txt",
                "ignore-a.md", "ignore-a.txt", "ignore-b.md", "ignore-b.txt");
    }

    @Test
    public void testGlobFileFilter() throws IOException {
        PathFinder.GlobFileFilter filter = new PathFinder.GlobFileFilter("**/include/**");
        fileFilterTest(filter);
    }

    @Test
    public void testGlobFileFilterPrefix() throws IOException {
        PathFinder.GlobFileFilter filter = new PathFinder.GlobFileFilter("glob:**/include/**");
        fileFilterTest(filter);
    }

    @Test
    public void testGlobFileFilterList() throws IOException {
        List<String> patterns = Arrays.asList("**/include/**");
        PathFinder.GlobFileFilter filter = new PathFinder.GlobFileFilter(patterns);
        fileFilterTest(filter);
    }

    @Test
    public void testGlobConstructorsCheckNull(){
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new PathFinder.GlobFileFilter((String) null))
                .withMessageContaining("The wildcard must not be null");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new PathFinder.GlobFileFilter((List<String>) null))
                .withMessageContaining("The wildcard must not be null");
    }

    @Test
    public void testRelativeFileFilter() throws IOException {
        PathFinder.GlobFileFilter filter = new PathFinder.RelativeFileFilter(projectBase(), "./**include/**");
        fileFilterTest(filter);
    }

    @Test
    public void testRelativeFileFilterPrefix() throws IOException {
        PathFinder.GlobFileFilter filter = new PathFinder.RelativeFileFilter(projectBase(), "glob:./**include/**");
        fileFilterTest(filter);
    }

    @Test
    public void testRelativeFileFilterList() throws IOException {
        List<String> patterns = Arrays.asList("./**include/**");
        PathFinder.GlobFileFilter filter = new PathFinder.RelativeFileFilter(projectBase(), patterns);
        fileFilterTest(filter);
    }

    @Test
    public void testRelativeFileFilterLists() throws IOException {
        List<String> patterns = Arrays.asList("./**include/**");
        PathFinder.GlobFileFilter filter = new PathFinder.RelativeFileFilter(Arrays.asList(projectBase()), patterns);
        fileFilterTest(filter);
    }

    @Test
    public void testRelativeFileFilterWithoutRelativePrefix() throws IOException {
        PathFinder.GlobFileFilter filter = new PathFinder.RelativeFileFilter(Arrays.asList(projectBase()), "**include/**");
        fileFilterTest(filter);
    }

    @Test
    public void testRelativeFileFilterConstructorsCheckNull(){
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new PathFinder.RelativeFileFilter((Path) null, "*"))
                .withMessageContaining("Relative base paths must not be null");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new PathFinder.RelativeFileFilter((List<Path>) null, "*"))
                .withMessageContaining("Relative base paths must not be null");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new PathFinder.RelativeFileFilter((Path) null, Arrays.asList("*")))
                .withMessageContaining("Relative base paths must not be null");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new PathFinder.RelativeFileFilter((List<Path>) null, Arrays.asList("*")))
                .withMessageContaining("Relative base paths must not be null");
    }

    protected void fileFilterTest(IOFileFilter filter) throws IOException {
        for(int i = 0; i < paths.size(); i++){
            File f = paths.get(i).toFile();
            File parent = paths.get(i).getParent().toFile();
            boolean expected = includePaths().contains(paths.get(i));
            assertThat(filter.accept(f)).isEqualTo(expected);
            assertThat(filter.accept(parent, f.getName())).isEqualTo(expected);
        }
    }

    protected void testPathFinder(PathFinder pathFinder, String... expectedFilenames) throws IOException {
        List<String> matchedFileNames = pathFinder.files().stream()
                .map(File::getName)
                .collect(Collectors.toList());
        assertThat(matchedFileNames)
                .containsExactlyInAnyOrder(expectedFilenames);
    }

    protected Path projectBase(){
        return Paths.get(folder.getRoot().getAbsolutePath(), "project");
    }

    protected Path moduleABase(){
        return Paths.get(projectBase().toString(),  "module-a");
    }

    protected Path moduleBBase(){
        return Paths.get(projectBase().toString(), "module-b");
    }

    protected List<Path> includePaths(){
        List<Path> created = new ArrayList<>();
        created.add(Paths.get(folder.getRoot().getAbsolutePath(), "project/module-a/include/readme-a.md".split("/")));
        created.add(Paths.get(folder.getRoot().getAbsolutePath(), "project/module-a/include/subdir/readme-a.txt".split("/")));
        created.add(Paths.get(folder.getRoot().getAbsolutePath(), "project/module-b/include/readme-b.md".split("/")));
        created.add(Paths.get(folder.getRoot().getAbsolutePath(), "project/module-b/include/subdir/readme-b.txt".split("/")));
        return created;
    }

    protected List<Path> excludePaths(){
        List<Path> created = new ArrayList<>();
        created.add(Paths.get(folder.getRoot().getAbsolutePath(), "project/module-a/exclude/ignore-a.md".split("/")));
        created.add(Paths.get(folder.getRoot().getAbsolutePath(), "project/module-a/exclude/subdir/ignore-a.txt".split("/")));
        created.add(Paths.get(folder.getRoot().getAbsolutePath(), "project/module-b/exclude/ignore-b.md".split("/")));
        created.add(Paths.get(folder.getRoot().getAbsolutePath(), "project/module-b/exclude/subdir/ignore-b.txt".split("/")));
        return created;
    }

    protected List<Path> setupTestFiles() throws IOException {
        List<Path> created = new ArrayList<>();
        created.addAll(includePaths());
        created.addAll(excludePaths());
        for(Path p : created){
            TestData.newFile(p);
        }
        return created;
    }
}
