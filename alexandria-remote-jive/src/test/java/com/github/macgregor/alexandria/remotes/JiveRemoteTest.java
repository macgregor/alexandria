package com.github.macgregor.alexandria.remotes;

import com.github.macgregor.alexandria.Alexandria;
import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Config.RemoteConfig;
import com.github.macgregor.alexandria.Context;
import com.github.macgregor.alexandria.Resources;
import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.exceptions.HttpException;
import com.github.macgregor.alexandria.markdown.JiveMarkdownConverter;
import com.github.macgregor.alexandria.markdown.MarkdownConverter;
import lombok.EqualsAndHashCode;
import okhttp3.HttpUrl;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.LogManager;

import static com.github.macgregor.alexandria.remotes.JiveRemote.JIVE_TRACKING_TAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class JiveRemoteTest {

    static{
        LogManager.getLogManager().reset();
    }

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testSyncWithJiveRemote() throws AlexandriaException {
        Config config = new Config();
        config.remote().clazz("com.github.macgregor.alexandria.remotes.JiveRemote");
        config.remote().baseUrl(Optional.of(""));
        config.remote().username(Optional.of(""));
        config.remote().password(Optional.of(""));

        Context context = new Context();
        context.configPath(Paths.get(folder.getRoot().toString(), ".alexandria"));
        context.config(config);

        Alexandria alexandria = new Alexandria();
        alexandria.context(context);
        alexandria.syncWithRemote();
    }

    @Test
    public void testFindDocumentUpdatesMetadataFromResponse() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237-Paged.json")),
                new MockResponse().setBody("{\"itemsPerPage\": 1,\n\"list\": [],\n\"startIndex\": 1\n}")
        ));

        Config.DocumentMetadata metadata = TestData.documentForUpdate(new Context(), folder);
        jiveRemote.findDocument(metadata);

        assertThat(metadata.lastUpdated().get())
                .isEqualTo(ZonedDateTime.parse("2018-06-22T18:42:59.652+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.createdOn().get())
                .isEqualTo(ZonedDateTime.parse("2016-03-21T15:07:34.533+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.extraProps().get().get("jiveParentUri")).isEqualTo("https://jive.com/groups/parent_group");
        assertThat(metadata.extraProps().get().get("jiveParentPlaceId")).isEqualTo("61562");
        assertThat(metadata.extraProps().get().get("jiveContentId")).isEqualTo("1278973");
    }

    @Test
    public void testFindDocumentThrowsAlexandriaExceptionWhenNotEnoughInformation() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Collections.emptyList());

        Config.DocumentMetadata metadata = TestData.documentForUpdate(new Context(), folder);
        metadata.extraProps().get().remove(JIVE_TRACKING_TAG);
        metadata.remoteUri(Optional.empty());
        assertThatThrownBy(() -> jiveRemote.findDocument(metadata)).isInstanceOf(AlexandriaException.class);
    }

    @Test
    public void testFindDocumentUnparsableResponse() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setBody(""));

        Config.DocumentMetadata metadata = TestData.documentForUpdate(new Context(), folder);

        assertThatThrownBy(() -> jiveRemote.findDocument(metadata))
                .isInstanceOf(HttpException.class)
                .hasMessageContaining("Unexpected error fetching next page from remote");
    }

    @Test
    public void testCreateDuplicateDocument() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237-Paged.json"))
        ));
        Config.DocumentMetadata metadata = TestData.documentForCreate(jiveRemote.alexandriaContext(), folder);
        jiveRemote.create(metadata);

        assertThat(metadata.lastUpdated().get())
                .isEqualTo(ZonedDateTime.parse("2018-06-22T18:42:59.652+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.createdOn().get())
                .isEqualTo(ZonedDateTime.parse("2016-03-21T15:07:34.533+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.extraProps().get().get("jiveParentUri")).isEqualTo("https://jive.com/groups/parent_group");
        assertThat(metadata.extraProps().get().get("jiveParentPlaceId")).isEqualTo("61562");
        assertThat(metadata.extraProps().get().get("jiveContentId")).isEqualTo("1278973");
    }

    @Test
    public void testCreateNullContentFindDocument() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = spy(setup(Arrays.asList(
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json")))));
        doReturn(null).when(jiveRemote).findDocument(any());

        Config.DocumentMetadata metadata = TestData.documentForCreate(jiveRemote.alexandriaContext(), folder);
        jiveRemote.create(metadata);

        assertThat(metadata.lastUpdated().get())
                .isEqualTo(ZonedDateTime.parse("2018-06-22T18:42:59.652+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.createdOn().get())
                .isEqualTo(ZonedDateTime.parse("2016-03-21T15:07:34.533+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.extraProps().get().get("jiveParentUri")).isEqualTo("https://jive.com/groups/parent_group");
        assertThat(metadata.extraProps().get().get("jiveParentPlaceId")).isEqualTo("61562");
        assertThat(metadata.extraProps().get().get("jiveContentId")).isEqualTo("1278973");
    }

    @Test
    public void testCreateHttpExceptionNoReponse() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = spy(setup(Arrays.asList(
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json")))));
        HttpException e = new HttpException();
        e.response(Optional.empty());
        doThrow(e).when(jiveRemote).findDocument(any());

        Config.DocumentMetadata metadata = TestData.documentForCreate(jiveRemote.alexandriaContext(), folder);

        assertThatThrownBy(() -> jiveRemote.create(metadata)).isEqualTo(e);
    }

    @Test
    public void testCreateHttpExceptionNon404() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = spy(setup(Arrays.asList(
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json")))));

        Response response = mock(Response.class);
        when(response.code()).thenReturn(500);
        HttpException e = new HttpException();
        e.response(Optional.of(response));
        doThrow(e).when(jiveRemote).findDocument(any());
        Config.DocumentMetadata metadata = TestData.documentForCreate(jiveRemote.alexandriaContext(), folder);

        assertThatThrownBy(() -> jiveRemote.create(metadata)).isInstanceOf(HttpException.class);
    }

    @Test
    public void testCreateUpdatesMetadataFromResponse() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setResponseCode(404),
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json"))));

        Config.DocumentMetadata metadata = TestData.documentForCreate(jiveRemote.alexandriaContext(), folder);
        jiveRemote.create(metadata);

        assertThat(metadata.lastUpdated().get())
                .isEqualTo(ZonedDateTime.parse("2018-06-22T18:42:59.652+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.createdOn().get())
                .isEqualTo(ZonedDateTime.parse("2016-03-21T15:07:34.533+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.extraProps().get().get("jiveParentUri")).isEqualTo("https://jive.com/groups/parent_group");
        assertThat(metadata.extraProps().get().get("jiveParentPlaceId")).isEqualTo("61562");
        assertThat(metadata.extraProps().get().get("jiveContentId")).isEqualTo("1278973");
    }

    @Test
    public void tesCreateUnparsableResponse() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setBody(""));

        Config.DocumentMetadata metadata = TestData.documentForCreate(jiveRemote.alexandriaContext(), folder);

        assertThatThrownBy(() -> jiveRemote.create(metadata))
                .isInstanceOf(HttpException.class)
                .hasMessageContaining("Unexpected error fetching next page from remote");
    }

    @Test
    public void testCreateFetchesParentPlaceFromRemote() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setResponseCode(404),
                new MockResponse().setBody(Resources.load("src/test/resources/parent_group-paged.json")),
                new MockResponse().setBody(Resources.load("src/test/resources/parent_group.json"))));

        Config.DocumentMetadata metadata = TestData.documentForCreate(jiveRemote.alexandriaContext(), folder);
        metadata.extraProps().get().put("jiveParentUri", "https://jive.com/places/parent_group");
        jiveRemote.create(metadata);

        assertThat(metadata.extraProps().get().get("jiveParentPlaceId")).isEqualTo("61562");
        assertThat(metadata.extraProps().get().get("jiveParentApiUri")).isEqualTo("https://jive.com/api/core/v3/places/61562");
    }

    @Test
    public void testCreateNoParentPlaceFound404() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setResponseCode(404),
                new MockResponse().setResponseCode(404),
                new MockResponse().setResponseCode(404),
                new MockResponse().setResponseCode(404),
                new MockResponse().setResponseCode(404),
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json"))
        ));

        Config.DocumentMetadata metadata = TestData.documentForCreate(jiveRemote.alexandriaContext(), folder);
        metadata.extraProps().get().put("jiveParentUri", "https://jive.com/places/parent_group");
        jiveRemote.create(metadata);
    }

    @Test
    public void testCreateNoParentPlaceFoundEmptyList() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setResponseCode(404),
                new MockResponse().setBody("{\"itemsPerPage\": 1,\n\"list\": [],\n\"startIndex\": 1\n}"),
                new MockResponse().setBody("{\"itemsPerPage\": 1,\n\"list\": [],\n\"startIndex\": 1\n}"),
                new MockResponse().setBody("{\"itemsPerPage\": 1,\n\"list\": [],\n\"startIndex\": 1\n}"),
                new MockResponse().setBody("{\"itemsPerPage\": 1,\n\"list\": [],\n\"startIndex\": 1\n}"),
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json"))
        ));

        Config.DocumentMetadata metadata = TestData.documentForCreate(jiveRemote.alexandriaContext(), folder);
        metadata.extraProps().get().put("jiveParentUri", "https://jive.com/places/parent_group");
        jiveRemote.create(metadata);
    }

    @Test
    public void testCreateMatchesParentPlaceDisplayName() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setResponseCode(404),
                new MockResponse().setBody(Resources.load("src/test/resources/parent_group-paged-nonunique.json")),
                new MockResponse().setBody(Resources.load("src/test/resources/parent_group.json"))));

        Config.DocumentMetadata metadata = TestData.documentForCreate(jiveRemote.alexandriaContext(), folder);
        metadata.extraProps().get().put("jiveParentUri", "https://jive.com/places/parent_group");
        jiveRemote.create(metadata);

        assertThat(metadata.extraProps().get().get("jiveParentPlaceId")).isEqualTo("61562");
        assertThat(metadata.extraProps().get().get("jiveParentApiUri")).isEqualTo("https://jive.com/api/core/v3/places/61562");
    }

    @Test
    public void testUpdateUpdatesMetadataFromResponse() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237-Paged.json")),
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json"))));

        Config.DocumentMetadata metadata = TestData.documentForUpdate(jiveRemote.alexandriaContext(), folder);
        jiveRemote.update(metadata);

        assertThat(metadata.lastUpdated().get())
                .isEqualTo(ZonedDateTime.parse("2018-06-22T18:42:59.652+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.createdOn().get())
                .isEqualTo(ZonedDateTime.parse("2016-03-21T15:07:34.533+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.extraProps().get().get("jiveParentUri")).isEqualTo("https://jive.com/groups/parent_group");
        assertThat(metadata.extraProps().get().get("jiveParentPlaceId")).isEqualTo("61562");
        assertThat(metadata.extraProps().get().get("jiveContentId")).isEqualTo("1278973");
    }

    @Test
    public void testUpdateDoesntLookupContentIdIfPresent() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json"))));

        Config.DocumentMetadata metadata = TestData.documentForUpdate(jiveRemote.alexandriaContext(), folder);
        metadata.extraProps().get().put("jiveContentId", "1234");
        jiveRemote.update(metadata);

        assertThat(metadata.lastUpdated().get())
                .isEqualTo(ZonedDateTime.parse("2018-06-22T18:42:59.652+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.createdOn().get())
                .isEqualTo(ZonedDateTime.parse("2016-03-21T15:07:34.533+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.extraProps().get().get("jiveParentUri")).isEqualTo("https://jive.com/groups/parent_group");
        assertThat(metadata.extraProps().get().get("jiveParentPlaceId")).isEqualTo("61562");
        assertThat(metadata.extraProps().get().get("jiveContentId")).isEqualTo("1278973");
    }

    @Test
    public void testUpdateSetsTrackingTagIfNeeded() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json"))));

        Config.DocumentMetadata metadata = TestData.documentForUpdate(jiveRemote.alexandriaContext(), folder);
        metadata.extraProps().get().put("jiveContentId", "1234");
        jiveRemote.update(metadata);

        assertThat(metadata.hasExtraProperty(JIVE_TRACKING_TAG)).isTrue();
    }

    @Test
    public void testUpdateHttpExceptionOnBadResponse() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setBody("asldknasd")));

        Config.DocumentMetadata metadata = TestData.documentForUpdate(jiveRemote.alexandriaContext(), folder);
        metadata.extraProps().get().put("jiveContentId", "1234");

        assertThatThrownBy(() -> jiveRemote.update(metadata)).isInstanceOf(HttpException.class);
    }

    @Test
    public void testUpdateFetchesParentPlaceFromRemote() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setBody(Resources.load("src/test/resources/parent_group-paged.json")),
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json"))));

        Config.DocumentMetadata metadata = TestData.documentForUpdate(jiveRemote.alexandriaContext(), folder);
        metadata.extraProps().get().put("jiveContentId", "1234");
        metadata.extraProps().get().put("jiveParentUri", "https://jive.com/places/parent_group");
        jiveRemote.update(metadata);


        assertThat(metadata.extraProps().get().get("jiveParentPlaceId")).isEqualTo("61562");
        assertThat(metadata.extraProps().get().get("jiveParentApiUri")).isEqualTo("https://jive.com/api/core/v3/places/61562");
    }

    @Test
    public void tesUpdateUnparsableResponse() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setBody(""));

        Config.DocumentMetadata metadata = TestData.documentForUpdate(jiveRemote.alexandriaContext(), folder);

        assertThatThrownBy(() -> jiveRemote.update(metadata))
                .isInstanceOf(HttpException.class)
                .hasMessageContaining("Unexpected error fetching next page from remote");
    }

    @Test
    public void testDeleteSetsDeletedDateTime() throws URISyntaxException, IOException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237-Paged.json")),
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json")),
                new MockResponse().setResponseCode(204)));

        Config.DocumentMetadata metadata = TestData.documentForDelete(jiveRemote.alexandriaContext(), folder);
        jiveRemote.delete(metadata);
        assertThat(metadata.deletedOn()).isPresent();
    }

    @Test
    public void testDeleteConsiders404AlreadyDeleted() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setResponseCode(404)));

        Config.DocumentMetadata metadata = TestData.documentForDelete(jiveRemote.alexandriaContext(), folder);
        metadata.setExtraProperty("jiveContentId", "1234");
        jiveRemote.delete(metadata);
        assertThat(metadata.deletedOn()).isPresent();
    }

    @Test
    public void testDeleteConsidersEmptyPagedResponseAlreadyDeleted() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setBody("{\"itemsPerPage\": 1,\n\"list\": [],\n\"startIndex\": 1\n}")
        ));

        Config.DocumentMetadata metadata = TestData.documentForDelete(jiveRemote.alexandriaContext(), folder);
        metadata.setExtraProperty("jiveContentId", "1234");
        jiveRemote.delete(metadata);
        assertThat(metadata.deletedOn()).isPresent();
    }

    @Test
    public void testDeleteDoesntLookupContentIdIfPresent() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237-Paged.json")),
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json")),
                new MockResponse().setResponseCode(204)));

        Config.DocumentMetadata metadata = TestData.documentForDelete(jiveRemote.alexandriaContext(), folder);
        metadata.setExtraProperty("jiveContentId", "1234");
        jiveRemote.delete(metadata);
        assertThat(metadata.deletedOn()).isPresent();
    }

    @Test
    public void testDeleteHttpExceptionNoReponse() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = spy(setup(Arrays.asList(
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json")))));
        HttpException e = new HttpException();
        e.response(Optional.empty());
        doThrow(e).when(jiveRemote).findDocument(any());

        Config.DocumentMetadata metadata = TestData.documentForCreate(jiveRemote.alexandriaContext(), folder);

        assertThatThrownBy(() -> jiveRemote.delete(metadata)).isEqualTo(e);
    }

    @Test
    public void testDeleteHttpExceptionNon404() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = spy(setup(Arrays.asList(
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json")))));

        Response response = mock(Response.class);
        when(response.code()).thenReturn(500);
        HttpException e = new HttpException();
        e.response(Optional.of(response));
        doThrow(e).when(jiveRemote).findDocument(any());
        Config.DocumentMetadata metadata = TestData.documentForCreate(jiveRemote.alexandriaContext(), folder);

        assertThatThrownBy(() -> jiveRemote.delete(metadata)).isInstanceOf(HttpException.class);
    }

    @Test
    public void testJiveRemoteValidateConfigRequiresBaseUrl(){
        RemoteConfig config = TestData.completeRemoteConfig();
        config.baseUrl(Optional.empty());
        JiveRemote remote = new JiveRemote(config);
        assertThatThrownBy(() -> remote.validateRemoteConfig())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("remote.baseUrl");
    }

    @Test
    public void testJiveRemoteValidateConfigRequiresUsername(){
        RemoteConfig config = TestData.completeRemoteConfig();
        config.username(Optional.empty());
        JiveRemote remote = new JiveRemote(config);
        assertThatThrownBy(() -> remote.validateRemoteConfig())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("remote.username");
    }

    @Test
    public void testJiveRemoteValidateConfigRequiresPassword(){
        RemoteConfig config = TestData.completeRemoteConfig();
        config.password(Optional.empty());
        JiveRemote remote = new JiveRemote(config);
        assertThatThrownBy(() -> remote.validateRemoteConfig())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("remote.password");
    }

    @Test
    public void testJiveRemoteUpdateMetadataRemoteUri() throws URISyntaxException {
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        JiveData.JiveContent content = new JiveData.JiveContent();
        JiveData.Link link = new JiveData.Link();
        link.ref = "http://www.google.com";
        link.allowed = Collections.singletonList("GET");
        content.resources.put("html", link);
        JiveRemote remote = new JiveRemote();
        remote.updateMetadata(metadata, content);
        assertThat(metadata.remoteUri().get()).isEqualTo(new URI("http://www.google.com"));
    }

    @Test
    public void testJiveRemoteUpdateMetadataRemoteUriBadLink() {
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        JiveData.JiveContent content = new JiveData.JiveContent();
        JiveData.Link link = new JiveData.Link();
        link.ref = null;
        link.allowed = Collections.singletonList("GET");
        content.resources.put("html", link);
        JiveRemote remote = new JiveRemote();
        remote.updateMetadata(metadata, content);
        assertThat(metadata.remoteUri()).isEmpty();
    }

    @Test
    public void testJiveRemoteUpdateMetadataRemoteUriContentResourcesNull() {
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        JiveData.JiveContent content = new JiveData.JiveContent();
        JiveData.Link link = new JiveData.Link();
        link.ref = null;
        link.allowed = Collections.singletonList("GET");
        content.resources = null;
        JiveRemote remote = new JiveRemote();
        remote.updateMetadata(metadata, content);
        assertThat(metadata.remoteUri()).isEmpty();
    }

    @Test
    public void testJiveRemoteUpdateMetadataRemoteUriContentResourcesNoHtmlAttribute() {
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        JiveData.JiveContent content = new JiveData.JiveContent();
        JiveData.Link link = new JiveData.Link();
        link.ref = null;
        link.allowed = Collections.singletonList("GET");
        JiveRemote remote = new JiveRemote();
        remote.updateMetadata(metadata, content);
        assertThat(metadata.remoteUri()).isEmpty();
    }

    @Test
    public void testJiveRemoteUpdateMetadataParentUrl() {
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        JiveData.JiveContent content = new JiveData.JiveContent();
        content.parentPlace = new JiveData.JiveContent.ParentPlace();
        content.parentPlace.html = "http://www.google.com";
        JiveRemote remote = new JiveRemote();
        remote.updateMetadata(metadata, content);
        assertThat(metadata.extraProps().get().get("jiveParentUri")).isEqualTo("http://www.google.com");
    }

    @Test
    public void testJiveRemoteUpdateMetadataParentId() {
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        JiveData.JiveContent content = new JiveData.JiveContent();
        content.parentPlace = new JiveData.JiveContent.ParentPlace();
        content.parentPlace.placeID = "1234";
        JiveRemote remote = new JiveRemote();
        remote.updateMetadata(metadata, content);
        assertThat(metadata.extraProps().get().get("jiveParentPlaceId")).isEqualTo("1234");
    }

    @Test
    public void testJiveRemoteUpdateMetadataContentId() {
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        JiveData.JiveContent content = new JiveData.JiveContent();
        content.contentID = "1234";
        JiveRemote remote = new JiveRemote();
        remote.updateMetadata(metadata, content);
        assertThat(metadata.extraProps().get().get("jiveContentId")).isEqualTo("1234");
    }

    @Test
    public void testJiveFindParentPlaceThrowsHttpExceptionOnBadResponse() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setBody("asldalskd")));

        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("DOC-1072237.md"));
        metadata.remoteUri(Optional.of(new URI("https://jive.com/docs/DOC-1072237")));
        metadata.extraProps().get().put("jiveContentId", "1234");
        metadata.extraProps().get().put("jiveParentUri", "https://jive.com/places/parent_group");
        jiveRemote.alexandriaContext().convertedPath(metadata, folder.newFile().toPath());

        assertThatThrownBy(() -> jiveRemote.findParentPlace(metadata)).isInstanceOf(HttpException.class);
    }

    @Test
    public void testJiveUpdateMetadataFromParentPlacePlaceId(){
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        JiveData.JivePlace place = new JiveData.JivePlace();
        place.placeID = null;
        assertThat(JiveRemote.updateMetadata(metadata, place)).isEqualTo(metadata);
        place.placeID = "";
        assertThat(JiveRemote.updateMetadata(metadata, place)).isEqualTo(metadata);
        place.placeID = "   ";
        assertThat(JiveRemote.updateMetadata(metadata, place)).isEqualTo(metadata);
        place.placeID = "foo";
        JiveRemote.updateMetadata(metadata, place);
        assertThat(metadata.extraProps().get().get("jiveParentPlaceId")).isEqualTo("foo");
    }

    @Test
    public void testJiveUpdateMetadataFromParentPlaceParentUri(){
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        JiveData.JivePlace place = new JiveData.JivePlace();
        place.resources = null;
        assertThat(JiveRemote.updateMetadata(metadata, place)).isEqualTo(metadata);
        place.resources = new HashMap<String, JiveData.Link>();
        assertThat(JiveRemote.updateMetadata(metadata, place)).isEqualTo(metadata);
        JiveData.Link link = new JiveData.Link();
        link.ref = "foo";
        link.allowed = Collections.singletonList("GET");
        place.resources.put("html", link);
        JiveRemote.updateMetadata(metadata, place);
        assertThat(metadata.extraProps().get().get("jiveParentUri")).isEqualTo("foo");
    }

    @Test
    public void testJiveUpdateMetadataFromParentPlaceParentApiUri(){
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        JiveData.JivePlace place = new JiveData.JivePlace();
        place.resources = null;
        assertThat(JiveRemote.updateMetadata(metadata, place)).isEqualTo(metadata);
        place.resources = new HashMap<String, JiveData.Link>();
        assertThat(JiveRemote.updateMetadata(metadata, place)).isEqualTo(metadata);
        JiveData.Link link = new JiveData.Link();
        link.ref = "foo";
        link.allowed = Collections.singletonList("GET");
        place.resources.put("self", link);
        JiveRemote.updateMetadata(metadata, place);
        assertThat(metadata.extraProps().get().get("jiveParentApiUri")).isEqualTo("foo");
    }

    @Test
    public void testJiveRemoteMarkdownConverter(){
        JiveRemote remote = new JiveRemote();
        JiveMarkdownConverter converter = new JiveMarkdownConverter();
        remote.markdownConverter(converter);
        assertThat(remote.markdownConverter()).isEqualTo(converter);
    }

    protected JiveRemote setup(MockResponse mockResponses) throws IOException {
        return setup(Collections.singletonList(mockResponses));
    }

    protected JiveRemote setup(List<MockResponse> mockResponses) throws IOException {
        MockWebServer server = new MockWebServer();
        for(MockResponse mockResponse : mockResponses) {
            server.enqueue(mockResponse);
        }
        server.start();

        HttpUrl baseUrl = server.url("api/core/v3");
        RemoteConfig config = new RemoteConfig();
        config.clazz("");
        config.baseUrl(Optional.of(baseUrl.toString()));
        config.username(Optional.of("user"));
        config.password(Optional.of("password"));
        config.requestTimeout(1);
        Context context = new Context();
        JiveRemote jiveRemote = new JiveRemote(config);
        jiveRemote.alexandriaContext(context);
        MarkdownConverter markdownConverter = new JiveMarkdownConverter();
        ((JiveMarkdownConverter) markdownConverter).alexandriaContext(context);
        jiveRemote.markdownConverter(markdownConverter);
        context.remote(Optional.of(jiveRemote));

        return jiveRemote;
    }

    @EqualsAndHashCode
    public static class PagedJivePlace{
        public List<JiveData.JivePlace> list = new ArrayList<>();
        public Integer startIndex;
        public Integer itemsPerPage;
    }

    @EqualsAndHashCode
    public static class PagedJiveContent{
        public List<JiveData.JiveContent> list = new ArrayList<>();
        public Integer startIndex;
        public Integer itemsPerPage;
    }
}
