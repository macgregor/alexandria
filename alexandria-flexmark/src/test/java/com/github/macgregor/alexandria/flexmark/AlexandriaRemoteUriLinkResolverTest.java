package com.github.macgregor.alexandria.flexmark;

import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Context;
import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.flexmark.links.LocalLinkRefProcessor;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.BasedSequenceImpl;
import org.assertj.core.api.AssertionsForClassTypes;
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

public class AlexandriaRemoteUriLinkResolverTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private AlexandriaRemoteUriLinkResolver remoteUriRelativeLinkResolver;
    private Context context;

    @Before
    public void setup() throws IOException {
        context = TestData.minimalContext(folder);
        remoteUriRelativeLinkResolver = new AlexandriaRemoteUriLinkResolver();
        remoteUriRelativeLinkResolver.alexandriaContext(context);
    }

    @Test
    public void testIsValidReturnsFalseIfContextNotSet(){
        remoteUriRelativeLinkResolver.alexandriaContext(null);
        assertThat(remoteUriRelativeLinkResolver.isValid("link text", "./foo/bar.txt")).isFalse();
    }

    @Test
    public void testIsValidFileDoesntExist() {
        assertThat(remoteUriRelativeLinkResolver.isValid("link text", "./readme.md")).isFalse();
    }

    @Test
    public void testIsValidPathToDir() throws IOException {
        File f = folder.newFolder("docs");
        assertThat(remoteUriRelativeLinkResolver.isValid("link text", "./docs")).isFalse();
    }

    @Test
    public void testIsValidFileExistsButNotIndexed() throws IOException {
        File f = folder.newFile("readme.md");
        assertThat(remoteUriRelativeLinkResolver.isValid("link text", "./readme.md")).isFalse();
    }

    @Test
    public void testIsValidFileExistsAndIndexed() throws IOException {
        File f = folder.newFile("readme.md");
        Config.DocumentMetadata metadata = TestData.minimalDocumentMetadata(context, f.toPath());
        assertThat(remoteUriRelativeLinkResolver.isValid("link text", "./readme.md")).isTrue();
    }

    @Test
    public void testIsValidFileExistsAndIndexedNoLeadingPathSegments() throws IOException {
        File f = folder.newFile("readme.md");
        Config.DocumentMetadata metadata = TestData.minimalDocumentMetadata(context, f.toPath());
        assertThat(remoteUriRelativeLinkResolver.isValid("link text", "readme.md")).isTrue();
    }

    @Test
    public void testIsValidFileExistsAndIndexedNoLeadingDot() throws IOException {
        File f = folder.newFile("readme.md");
        Config.DocumentMetadata metadata = TestData.minimalDocumentMetadata(context, f.toPath());
        assertThat(remoteUriRelativeLinkResolver.isValid("link text", "/readme.md")).isFalse();
    }

    @Test
    public void testResolveThrowsExceptionIfContextNotSet(){
        remoteUriRelativeLinkResolver.alexandriaContext(null);
        assertThatThrownBy(() -> remoteUriRelativeLinkResolver.resolve("link text", "./foo/bar.txt"))
                .isInstanceOf(AlexandriaException.class);
    }

    @Test
    public void testResolveRemoteUriPresent() throws IOException, URISyntaxException {
        File f = folder.newFile("readme.md");
        URI remoteUri = new URI("https://www.google.com");
        Config.DocumentMetadata metadata = TestData.minimalDocumentMetadata(context, f.toPath());
        metadata.remoteUri(Optional.of(remoteUri));
        assertThat(remoteUriRelativeLinkResolver.resolve("link text", "./readme.md")).isEqualTo(remoteUri);
    }

    @Test
    public void testResolveRemoteUriAbsent() throws IOException, URISyntaxException {
        File f = folder.newFile("readme.md");
        URI localUri = new URI("./readme.md");
        Config.DocumentMetadata metadata = TestData.minimalDocumentMetadata(context, f.toPath());
        metadata.remoteUri(Optional.empty());
        assertThat(remoteUriRelativeLinkResolver.resolve("link text", "./readme.md")).isEqualTo(localUri);
    }

    @Test
    public void testRelativeLinkProcessorMatchesBasicGithubStyleRelativeLinks(){
        assertThat(remoteUriRelativeLinkResolver.wants("[link text](./foo.txt)"));
    }

    @Test
    public void testRelativeLinkProcessorMatchesBasicGithubStyleRelativeLinkNoText(){
        assertThat(remoteUriRelativeLinkResolver.wants("[](./foo.txt)"));
    }

    @Test
    public void testRelativeLinkProcessorMatchesBasicGithubStyleRelativeLinksNoDot(){
        assertThat(remoteUriRelativeLinkResolver.wants("[link text](foo.txt)"));
        BasedSequence chars = BasedSequenceImpl.of("[link text](foo.txt)");
    }

    @Test
    public void testRelativeLinkProcessorDoesntMatchNonGithubStyleLink(){
        assertThat(remoteUriRelativeLinkResolver.wants("[link text|./foo.txt]")).isFalse();
    }

    @Test
    public void testRelativeLinkProcessorDoesntMatchUrl(){
        assertThat(remoteUriRelativeLinkResolver.wants("[link text](https://www.google.com)")).isFalse();
    }

    @Test
    public void testRelativeLinkProcessorDoesntMatchAbsolutePath(){
        assertThat(remoteUriRelativeLinkResolver.wants("[link text](/foo/bar.txt)")).isFalse();
    }

    @Test
    public void testRelativeLinkProcessorDoesntMatchNull(){
        assertThat(remoteUriRelativeLinkResolver.wants(null)).isFalse();
    }

    @Test
    public void testRelativeLinkProcessorDoesntMatchEmpty(){
        assertThat(remoteUriRelativeLinkResolver.wants("")).isFalse();
    }
}
