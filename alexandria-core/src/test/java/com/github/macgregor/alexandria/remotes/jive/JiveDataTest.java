package com.github.macgregor.alexandria.remotes.jive;

import com.github.macgregor.alexandria.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class JiveDataTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    public Context context;
    public Config.DocumentMetadata metadata;

    @Test
    public void voidTestJivePagedContentParsing() throws IOException {
        JiveData.PagedJiveContent parsed = Jackson.jsonMapper().readValue(Resources.load("src/test/resources/DOC-1072237-Paged.json"), JiveData.PagedJiveContent.class);

        assertThat(parsed).isEqualToComparingFieldByFieldRecursively(TestData.expectedPagedJiveContent());
    }

    @Test
    public void testJiveUpdateMetadataHandlesMissingMalformedPagedContent(){
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        assertThat(JiveRemote.updateMetadata(metadata, (JiveData.PagedJiveContent) null)).isEqualTo(metadata);
        JiveData.PagedJiveContent content = new JiveData.PagedJiveContent();
        content.list = null;
        assertThat(JiveRemote.updateMetadata(metadata, content)).isEqualTo(metadata);
        content.list = new ArrayList<>();
        assertThat(JiveRemote.updateMetadata(metadata, content)).isEqualTo(metadata);
    }

    @Test
    public void testJiveBuildsDocumentPostBody() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = TestData.documentForCreate(context, folder);
        context.config().metadata(Optional.of(Collections.singletonList(metadata)));

        JiveData.JiveContent jiveContent = JiveData.documentPostBody(context, metadata);

        assertThat(jiveContent.parentPlace).isNull();
        assertThat(jiveContent.subject).isEqualTo(metadata.title());
        assertThat(jiveContent.content.text).isEqualTo(Resources.load(context.convertedPath(metadata).get().toString()));
        assertThat(jiveContent.type).isEqualTo("document");
        assertThat(jiveContent.typeCode).isEqualTo(102);
    }

    @Test
    public void testJiveBuildsDocumentPostBodyWithJiveContentId() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = TestData.documentForCreate(context, folder);
        context.config().metadata(Optional.of(Collections.singletonList(metadata)));
        metadata.extraProps().get().put("jiveContentId", "1234");

        JiveData.JiveContent jiveContent = JiveData.documentPostBody(context, metadata);

        assertThat(jiveContent.contentID).isEqualTo("1234");
    }

    @Test
    public void testJiveBuildsDocumentPostBodyWithJiveParentApiUri() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = TestData.documentForCreate(context, folder);
        context.config().metadata(Optional.of(Collections.singletonList(metadata)));
        metadata.extraProps().get().put("jiveParentApiUri", "wwww.google.com");

        JiveData.JiveContent jiveContent = JiveData.documentPostBody(context, metadata);

        assertThat(jiveContent.parent).isEqualTo("wwww.google.com");
    }

    @Test
    public void testJiveBuildsDocumentPostBodyWithTags() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = TestData.documentForCreate(context, folder);
        context.config().metadata(Optional.of(Collections.singletonList(metadata)));
        metadata.tags(Optional.of(Collections.singletonList("foo")));

        JiveData.JiveContent jiveContent = JiveData.documentPostBody(context, metadata);

        assertThat(jiveContent.tags).containsExactlyInAnyOrder("foo");
    }
}
