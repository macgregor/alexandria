package com.github.macgregor.alexandria.markdown;

import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Context;
import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.remotes.TestData;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JiveRelativeLinkResolverTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private JivaRelativeLinkResolver jivaRelativeLinkResolver;
    private Context context;

    @Before
    public void setup() throws IOException {
        context = TestData.minimalContext(folder);
        jivaRelativeLinkResolver = new JivaRelativeLinkResolver();
        jivaRelativeLinkResolver.alexandriaContext(context);
    }

    @Test
    public void testIsValidReturnsFalseIfContextNotSet(){
        jivaRelativeLinkResolver.alexandriaContext(null);
        assertThat(jivaRelativeLinkResolver.isValid("link text", "./foo/bar.txt")).isFalse();
    }

    @Test
    public void testIsValidFileDoesntExist() {
        assertThat(jivaRelativeLinkResolver.isValid("link text", "./readme.md")).isFalse();
    }

    @Test
    public void testIsValidPathToDir() throws IOException {
        File f = folder.newFolder("docs");
        assertThat(jivaRelativeLinkResolver.isValid("link text", "./docs")).isFalse();
    }

    @Test
    public void testIsValidFileExistsButNotIndexed() throws IOException {
        File f = folder.newFile("readme.md");
        assertThat(jivaRelativeLinkResolver.isValid("link text", "./readme.md")).isFalse();
    }

    @Test
    public void testIsValidFileExistsAndIndexed() throws IOException {
        File f = folder.newFile("readme.md");
        Config.DocumentMetadata metadata = TestData.minimalDocumentMetadata(context, f.toPath());
        assertThat(jivaRelativeLinkResolver.isValid("link text", "./readme.md")).isTrue();
    }

    @Test
    public void testIsValidFileExistsAndIndexedNoLeadingPathSegments() throws IOException {
        File f = folder.newFile("readme.md");
        Config.DocumentMetadata metadata = TestData.minimalDocumentMetadata(context, f.toPath());
        assertThat(jivaRelativeLinkResolver.isValid("link text", "readme.md")).isTrue();
    }

    @Test
    public void testIsValidFileExistsAndIndexedNoLeadingDot() throws IOException {
        File f = folder.newFile("readme.md");
        Config.DocumentMetadata metadata = TestData.minimalDocumentMetadata(context, f.toPath());
        assertThat(jivaRelativeLinkResolver.isValid("link text", "/readme.md")).isFalse();
    }

    @Test
    public void testResolveThrowsExceptionIfContextNotSet(){
        jivaRelativeLinkResolver.alexandriaContext(null);
        assertThatThrownBy(() -> jivaRelativeLinkResolver.resolve("link text", "./foo/bar.txt"))
                .isInstanceOf(AlexandriaException.class);
    }

    @Test
    public void testResolveRemoteUriPresent() throws IOException, URISyntaxException {
        File f = folder.newFile("readme.md");
        URI remoteUri = new URI("https://www.google.com");
        Config.DocumentMetadata metadata = TestData.minimalDocumentMetadata(context, f.toPath());
        metadata.remoteUri(Optional.of(remoteUri));
        assertThat(jivaRelativeLinkResolver.resolve("link text", "./readme.md")).isEqualTo(remoteUri);
    }

    @Test
    public void testResolveRemoteUriAbsent() throws IOException, URISyntaxException {
        File f = folder.newFile("readme.md");
        URI localUri = new URI("./readme.md");
        Config.DocumentMetadata metadata = TestData.minimalDocumentMetadata(context, f.toPath());
        metadata.remoteUri(Optional.empty());
        assertThat(jivaRelativeLinkResolver.resolve("link text", "./readme.md")).isEqualTo(localUri);
    }
}
