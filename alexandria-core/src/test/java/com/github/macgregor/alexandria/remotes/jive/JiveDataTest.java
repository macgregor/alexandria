package com.github.macgregor.alexandria.remotes.jive;

import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Context;
import com.github.macgregor.alexandria.Resources;
import com.github.macgregor.alexandria.TestData;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class JiveDataTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    public Context context;
    public Config.DocumentMetadata metadata;

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
