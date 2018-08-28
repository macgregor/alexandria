package com.github.macgregor.alexandria.remotes;

import com.github.macgregor.alexandria.*;
import com.github.macgregor.alexandria.Config.RemoteConfig;
import com.github.macgregor.alexandria.exceptions.HttpException;
import okhttp3.HttpUrl;
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

import static org.assertj.core.api.Assertions.*;

public class JiveRemoteTest {

    static{
        LogManager.getLogManager().reset();
    }

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void voidTestJivePagedContentParsing() throws IOException {
        JiveRemote.PagedJiveContent parsed = Jackson.jsonMapper().readValue(Resources.load("src/test/resources/DOC-1072237-Paged.json"), JiveRemote.PagedJiveContent.class);

        assertThat(parsed).isEqualToComparingFieldByFieldRecursively(expectedPagedJiveContent());
    }

    @Test
    public void testSyncMetadata400() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setResponseCode(400));

        Config.DocumentMetadata metadata = TestData.documentForUpdate(new Context(), folder);

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.findDocument(new Context(), metadata))
                .withMessageContaining("Bad request");
    }

    @Test
    public void testSyncMetadata500() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setResponseCode(500));

        Config.DocumentMetadata metadata = TestData.documentForUpdate(new Context(), folder);

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.findDocument(new Context(), metadata))
                .withMessageContaining("Unexpected status code");
    }

    @Test
    public void testSyncMetadataUpdatesMetadataFromResponse() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237-Paged.json")));

        Config.DocumentMetadata metadata = TestData.documentForUpdate(new Context(), folder);
        jiveRemote.findDocument(new Context(), metadata);

        assertThat(metadata.lastUpdated().get())
                .isEqualTo(ZonedDateTime.parse("2018-06-22T18:42:59.652+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.createdOn().get())
                .isEqualTo(ZonedDateTime.parse("2016-03-21T15:07:34.533+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.extraProps().get().get("jiveParentUri")).isEqualTo("https://jive.com/groups/parent_group");
        assertThat(metadata.extraProps().get().get("jiveParentPlaceId")).isEqualTo("61562");
        assertThat(metadata.extraProps().get().get("jiveContentId")).isEqualTo("1278973");
    }

    @Test
    public void testSyncMetadataUnparsableResponse() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setBody(""));

        Config.DocumentMetadata metadata = TestData.documentForUpdate(new Context(), folder);

        assertThatThrownBy(() -> jiveRemote.findDocument(new Context(), metadata))
                .isInstanceOf(HttpException.class)
                .hasMessageContaining("Cannot parse response content");
    }

    @Test
    public void testCreateUpdatesMetadataFromResponse() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setResponseCode(404),
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json"))));

        Context context = new Context();
        Config.DocumentMetadata metadata = TestData.documentForCreate(context, folder);
        jiveRemote.create(context, metadata);

        assertThat(metadata.lastUpdated().get())
                .isEqualTo(ZonedDateTime.parse("2018-06-22T18:42:59.652+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.createdOn().get())
                .isEqualTo(ZonedDateTime.parse("2016-03-21T15:07:34.533+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.extraProps().get().get("jiveParentUri")).isEqualTo("https://jive.com/groups/parent_group");
        assertThat(metadata.extraProps().get().get("jiveParentPlaceId")).isEqualTo("61562");
        assertThat(metadata.extraProps().get().get("jiveContentId")).isEqualTo("1278973");
    }

    @Test
    public void testCreate400() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setResponseCode(400));

        Context context = new Context();
        Config.DocumentMetadata metadata = TestData.documentForCreate(context, folder);

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.create(context, metadata))
                .withMessageContaining("Bad request");
    }

    @Test
    public void testCreate409() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setResponseCode(409));

        Context context = new Context();
        Config.DocumentMetadata metadata = TestData.documentForCreate(context, folder);

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.create(context, metadata))
                .withMessageContaining("Document conflicts with existing document");
    }

    @Test
    public void testCreate403() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setResponseCode(403));

        Context context = new Context();
        Config.DocumentMetadata metadata = TestData.documentForCreate(context, folder);

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.create(context, metadata))
                .withMessageContaining("Unauthorized to access document");
    }

    @Test
    public void tesCreateUnparsableResponse() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setBody(""));

        Context context = new Context();
        Config.DocumentMetadata metadata = TestData.documentForCreate(context, folder);

        assertThatThrownBy(() -> jiveRemote.create(context, metadata))
                .isInstanceOf(HttpException.class)
                .hasMessageContaining("Cannot parse response content");
    }

    @Test
    public void testCreateFetchesParentPlaceFromRemote() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setResponseCode(404),
                new MockResponse().setBody(Resources.load("src/test/resources/parent_group-paged.json")),
                new MockResponse().setBody(Resources.load("src/test/resources/parent_group.json"))));

        Context context = new Context();
        Config.DocumentMetadata metadata = TestData.documentForCreate(context, folder);
        metadata.extraProps().get().put("jiveParentUri", "https://jive.com/places/parent_group");
        jiveRemote.create(context, metadata);

        assertThat(metadata.extraProps().get().get("jiveParentPlaceId")).isEqualTo("61562");
        assertThat(metadata.extraProps().get().get("jiveParentApiUri")).isEqualTo("https://jive.com/api/core/v3/places/61562");
    }

    @Test
    public void testUpdateUpdatesMetadataFromResponse() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237-Paged.json")),
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json"))));

        Context context = new Context();
        Config.DocumentMetadata metadata = TestData.documentForUpdate(context, folder);
        jiveRemote.update(context, metadata);

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

        Context context = new Context();
        Config.DocumentMetadata metadata = TestData.documentForUpdate(context, folder);
        metadata.extraProps().get().put("jiveContentId", "1234");
        jiveRemote.update(context, metadata);

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

        Context context = new Context();
        Config.DocumentMetadata metadata = TestData.documentForUpdate(context, folder);
        metadata.extraProps().get().put("jiveContentId", "1234");
        jiveRemote.update(context, metadata);

        assertThat(metadata.hasExtraProperty(JiveRemote.JIVE_TRACKING_TAG)).isTrue();
    }

    @Test
    public void testUpdateHttpExceptionOnBadResponse() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setBody("asldknasd")));

        Context context = new Context();
        Config.DocumentMetadata metadata = TestData.documentForUpdate(context, folder);
        metadata.extraProps().get().put("jiveContentId", "1234");

        assertThatThrownBy(() -> jiveRemote.update(context, metadata)).isInstanceOf(HttpException.class);
    }

    @Test
    public void testUpdateFetchesParentPlaceFromRemote() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setBody(Resources.load("src/test/resources/parent_group-paged.json")),
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json"))));

        Context context = new Context();
        Config.DocumentMetadata metadata = TestData.documentForUpdate(context, folder);
        metadata.extraProps().get().put("jiveContentId", "1234");
        metadata.extraProps().get().put("jiveParentUri", "https://jive.com/places/parent_group");
        jiveRemote.update(context, metadata);


        assertThat(metadata.extraProps().get().get("jiveParentPlaceId")).isEqualTo("61562");
        assertThat(metadata.extraProps().get().get("jiveParentApiUri")).isEqualTo("https://jive.com/api/core/v3/places/61562");
    }

    @Test
    public void testUpdate400() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setResponseCode(400));

        Context context = new Context();
        Config.DocumentMetadata metadata = TestData.documentForUpdate(context, folder);

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.update(context, metadata))
                .withMessageContaining("Bad request");
    }

    @Test
    public void testUpdate409() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setResponseCode(409));

        Context context = new Context();
        Config.DocumentMetadata metadata = TestData.documentForUpdate(context, folder);

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.update(context, metadata))
                .withMessageContaining("Document conflicts with existing document");
    }

    @Test
    public void testUpdate403() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setResponseCode(403));

        Context context = new Context();
        Config.DocumentMetadata metadata = TestData.documentForUpdate(context, folder);

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.update(context, metadata))
                .withMessageContaining("Unauthorized to access document");
    }

    @Test
    public void testUpdate404() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setResponseCode(404));

        Context context = new Context();
        Config.DocumentMetadata metadata = TestData.documentForUpdate(context, folder);

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.update(context, metadata))
                .withMessageContaining("Document doesnt exist");
    }

    @Test
    public void tesUpdateUnparsableResponse() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setBody(""));

        Context context = new Context();
        Config.DocumentMetadata metadata = TestData.documentForUpdate(context, folder);

        assertThatThrownBy(() -> jiveRemote.update(context, metadata))
                .isInstanceOf(HttpException.class)
                .hasMessageContaining("Cannot parse response content");
    }

    @Test
    public void testDeleteSetsDeletedDateTime() throws URISyntaxException, IOException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237-Paged.json")),
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json")),
                new MockResponse().setResponseCode(204)));

        Config.DocumentMetadata metadata = TestData.documentForDelete(new Context(), folder);
        jiveRemote.delete(new Context(), metadata);
        assertThat(metadata.deletedOn()).isPresent();
    }

    @Test
    public void testDeleteAssumes404WhenSearchingForDocMeansDeleted() throws URISyntaxException, IOException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237-Paged.json")),
                new MockResponse().setResponseCode(404)));

        Config.DocumentMetadata metadata = TestData.documentForDelete(new Context(), folder);
        jiveRemote.delete(new Context(), metadata);
        assertThat(metadata.deletedOn()).isPresent();
    }

    @Test
    public void testDeleteDoesntLookupContentIdIfPresent() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237-Paged.json")),
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json")),
                new MockResponse().setResponseCode(204)));

        Config.DocumentMetadata metadata = TestData.documentForDelete(new Context(), folder);
        metadata.setExtraProperty("jiveContentId", "1234");
        jiveRemote.delete(new Context(), metadata);
        assertThat(metadata.deletedOn()).isPresent();
    }

    @Test
    public void testDelete400() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setResponseCode(400));

        Config.DocumentMetadata metadata = TestData.documentForDelete(new Context(), folder);

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.delete(new Context(), metadata))
                .withMessageContaining("Bad request");
    }

    @Test
    public void testDelete403() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setResponseCode(403));

        Config.DocumentMetadata metadata = TestData.documentForDelete(new Context(), folder);

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.delete(new Context(), metadata))
                .withMessageContaining("Unauthorized to access document");
    }

    @Test
    public void testDelete404() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237-Paged.json")),
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json")),
                new MockResponse().setResponseCode(404)));

        Config.DocumentMetadata metadata = TestData.documentForDelete(new Context(), folder);

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.delete(new Context(), metadata))
                .withMessageContaining("Document doesnt exist");
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
        JiveRemote.JiveContent content = new JiveRemote.JiveContent();
        JiveRemote.Link link = new JiveRemote.Link();
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
        JiveRemote.JiveContent content = new JiveRemote.JiveContent();
        JiveRemote.Link link = new JiveRemote.Link();
        link.ref = null;
        link.allowed = Collections.singletonList("GET");
        content.resources.put("html", link);
        JiveRemote remote = new JiveRemote();
        remote.updateMetadata(metadata, content);
        assertThat(metadata.remoteUri()).isEmpty();
    }

    @Test
    public void testJiveRemoteUpdateMetadataParentUrl() {
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        JiveRemote.JiveContent content = new JiveRemote.JiveContent();
        content.parentPlace = new JiveRemote.JiveContent.ParentPlace();
        content.parentPlace.html = "http://www.google.com";
        JiveRemote remote = new JiveRemote();
        remote.updateMetadata(metadata, content);
        assertThat(metadata.extraProps().get().get("jiveParentUri")).isEqualTo("http://www.google.com");
    }

    @Test
    public void testJiveRemoteUpdateMetadataParentId() {
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        JiveRemote.JiveContent content = new JiveRemote.JiveContent();
        content.parentPlace = new JiveRemote.JiveContent.ParentPlace();
        content.parentPlace.placeID = "1234";
        JiveRemote remote = new JiveRemote();
        remote.updateMetadata(metadata, content);
        assertThat(metadata.extraProps().get().get("jiveParentPlaceId")).isEqualTo("1234");
    }

    @Test
    public void testJiveRemoteUpdateMetadataContentId() {
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        JiveRemote.JiveContent content = new JiveRemote.JiveContent();
        content.contentID = "1234";
        JiveRemote remote = new JiveRemote();
        remote.updateMetadata(metadata, content);
        assertThat(metadata.extraProps().get().get("jiveContentId")).isEqualTo("1234");
    }

    @Test
    public void testJiveRemoteUpdateMetadataPagedEmpty() {
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        JiveRemote.PagedJiveContent content = new JiveRemote.PagedJiveContent();
        content.list = Collections.EMPTY_LIST;
        JiveRemote remote = new JiveRemote();
        remote.updateMetadata(metadata, content);
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
        Context context = new Context();
        context.convertedPath(metadata, folder.newFile().toPath());

        assertThatThrownBy(() -> jiveRemote.findParentPlace(context, metadata)).isInstanceOf(HttpException.class);
    }

    @Test
    public void testJiveObjectIdThrowsIllegalArgumentOnBadPattern(){
        assertThatThrownBy(() -> JiveRemote.jiveObjectId(new URI("asldkasd"))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testJiveParentPlaceNameThrowsIllegalArgumentOnBadPattern(){
        assertThatThrownBy(() -> JiveRemote.jiveParentPlaceName("asldkasd")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testJiveUpdateMetadataHandlesMissingMalformedPagedParent(){
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        assertThat(JiveRemote.updateMetadata(metadata, (JiveRemote.PagedJivePlace) null)).isEqualTo(metadata);
        JiveRemote.PagedJivePlace parentPlace = new JiveRemote.PagedJivePlace();
        parentPlace.list = null;
        assertThat(JiveRemote.updateMetadata(metadata, parentPlace)).isEqualTo(metadata);
        parentPlace.list = new ArrayList<>();
        assertThat(JiveRemote.updateMetadata(metadata, parentPlace)).isEqualTo(metadata);
    }

    @Test
    public void testJiveUpdateMetadataFromParentPlacePlaceId(){
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("foo"));
        JiveRemote.JivePlace place = new JiveRemote.JivePlace();
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
        JiveRemote.JivePlace place = new JiveRemote.JivePlace();
        place.resources = null;
        assertThat(JiveRemote.updateMetadata(metadata, place)).isEqualTo(metadata);
        place.resources = new HashMap<String, JiveRemote.Link>();
        assertThat(JiveRemote.updateMetadata(metadata, place)).isEqualTo(metadata);
        JiveRemote.Link link = new JiveRemote.Link();
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
        JiveRemote.JivePlace place = new JiveRemote.JivePlace();
        place.resources = null;
        assertThat(JiveRemote.updateMetadata(metadata, place)).isEqualTo(metadata);
        place.resources = new HashMap<String, JiveRemote.Link>();
        assertThat(JiveRemote.updateMetadata(metadata, place)).isEqualTo(metadata);
        JiveRemote.Link link = new JiveRemote.Link();
        link.ref = "foo";
        link.allowed = Collections.singletonList("GET");
        place.resources.put("self", link);
        JiveRemote.updateMetadata(metadata, place);
        assertThat(metadata.extraProps().get().get("jiveParentApiUri")).isEqualTo("foo");
    }

    @Test
    public void testJiveUpdateMetadataHandlesMissingMalformedPagedContent(){
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        assertThat(JiveRemote.updateMetadata(metadata, (JiveRemote.PagedJiveContent) null)).isEqualTo(metadata);
        JiveRemote.PagedJiveContent content = new JiveRemote.PagedJiveContent();
        content.list = null;
        assertThat(JiveRemote.updateMetadata(metadata, content)).isEqualTo(metadata);
        content.list = new ArrayList<>();
        assertThat(JiveRemote.updateMetadata(metadata, content)).isEqualTo(metadata);
    }

    @Test
    public void testJiveDoesntNeedContentIdWhenNoRemoteUri(){
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.remoteUri(Optional.empty());
        assertThat(JiveRemote.needsContentId(metadata)).isFalse();
    }

    @Test
    public void testJiveNeedsContentIdWhenRemoteUriWithoutExtraProps() throws URISyntaxException {
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.remoteUri(Optional.of(new URI("foo")));
        metadata.extraProps(Optional.empty());
        assertThat(JiveRemote.needsContentId(metadata)).isTrue();
    }

    @Test
    public void testJiveNeedsContentIdWhenRemoteUriWithoutJiveContentIdProperty() throws URISyntaxException {
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.remoteUri(Optional.of(new URI("foo")));
        metadata.extraProps(Optional.of(Collections.emptyMap()));
        assertThat(JiveRemote.needsContentId(metadata)).isTrue();
    }

    @Test
    public void testJiveDoesntNeedContentIdWhenRemoteUriWithJiveContentIdProperty() throws URISyntaxException {
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.remoteUri(Optional.of(new URI("foo")));
        metadata.extraProps(Optional.of(Collections.singletonMap("jiveContentId", "1234")));
        assertThat(JiveRemote.needsContentId(metadata)).isFalse();
    }

    @Test
    public void testJiveDoesntNeedParentPlaceUriWhenNoExtraProps(){
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.extraProps(Optional.empty());
        assertThat(JiveRemote.needsParentPlaceUri(metadata)).isFalse();
    }

    @Test
    public void testJiveNeedsParentPlaceUriWhenNoJiveParentApiUriIsPresent(){
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.extraProps(Optional.of(Collections.singletonMap("jiveParentUri", "foo")));
        assertThat(JiveRemote.needsParentPlaceUri(metadata)).isTrue();
    }

    @Test
    public void testJiveDoesntNeedParentPlaceUriWhenJiveParentApiUriIsPresent(){
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        Map<String, String> extraProps = new HashMap<>();
        extraProps.put("jiveParentApiUri", "foo");
        extraProps.put("jiveParentApiUri", "foo");
        metadata.extraProps(Optional.of(extraProps));
        assertThat(JiveRemote.needsParentPlaceUri(metadata)).isFalse();
    }

    @Test
    public void testJiveBuildsDocumentPostBody() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = TestData.documentForCreate(context, folder);
        context.config().metadata(Optional.of(Collections.singletonList(metadata)));

        String postBody = JiveRemote.documentPostBody(context, metadata);
        JiveRemote.JiveContent jiveContent = Jackson.jsonMapper().readValue(postBody, JiveRemote.JiveContent.class);

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

        String postBody = JiveRemote.documentPostBody(context, metadata);
        JiveRemote.JiveContent jiveContent = Jackson.jsonMapper().readValue(postBody, JiveRemote.JiveContent.class);

        assertThat(jiveContent.contentID).isEqualTo("1234");
    }

    @Test
    public void testJiveBuildsDocumentPostBodyWithJiveParentApiUri() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = TestData.documentForCreate(context, folder);
        context.config().metadata(Optional.of(Collections.singletonList(metadata)));
        metadata.extraProps().get().put("jiveParentApiUri", "wwww.google.com");

        String postBody = JiveRemote.documentPostBody(context, metadata);
        JiveRemote.JiveContent jiveContent = Jackson.jsonMapper().readValue(postBody, JiveRemote.JiveContent.class);

        assertThat(jiveContent.parent).isEqualTo("wwww.google.com");
    }

    @Test
    public void testJiveBuildsDocumentPostBodyWithTags() throws IOException, URISyntaxException {
        Context context = TestData.minimalContext(folder);
        Config.DocumentMetadata metadata = TestData.documentForCreate(context, folder);
        context.config().metadata(Optional.of(Collections.singletonList(metadata)));
        metadata.tags(Optional.of(Collections.singletonList("foo")));

        String postBody = JiveRemote.documentPostBody(context, metadata);
        JiveRemote.JiveContent jiveContent = Jackson.jsonMapper().readValue(postBody, JiveRemote.JiveContent.class);

        assertThat(jiveContent.tags).containsExactlyInAnyOrder("foo");
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
        config.baseUrl(Optional.of(baseUrl.toString()));
        config.username(Optional.of("user"));
        config.password(Optional.of("password"));
        config.requestTimeout(1);
        JiveRemote jiveRemote = new JiveRemote(config);

        return jiveRemote;
    }

    private JiveRemote.PagedJiveContent expectedPagedJiveContent(){
        JiveRemote.JiveContent jiveContent = new JiveRemote.JiveContent();
        jiveContent.id = 1072237;
        jiveContent.published = ZonedDateTime.parse("2016-03-21T15:07:34.533+0000", DateTimeFormatter.ofPattern(Config.ALEXANDRIA_DATETIME_PATTERN));
        jiveContent.updated = ZonedDateTime.parse("2018-06-22T18:42:59.652+0000", DateTimeFormatter.ofPattern(Config.ALEXANDRIA_DATETIME_PATTERN));
        jiveContent.tags = Arrays.asList("foo", "bar", "baz");
        jiveContent.contentID = "1278973";
        jiveContent.parent = "https://jive.com/api/core/v3/places/61562";
        jiveContent.subject = "Document Title";
        jiveContent.type = "document";
        jiveContent.typeCode = 102;

        JiveRemote.JiveContent.Content content = new JiveRemote.JiveContent.Content();
        content.editable = true;
        content.type = "text/html";
        content.text = "<body></body>";
        jiveContent.content = content;

        JiveRemote.JiveContent.ParentPlace parentPlace = new JiveRemote.JiveContent.ParentPlace();
        parentPlace.id = 2276;
        parentPlace.html = "https://jive.com/groups/parent_group";
        parentPlace.placeID = "61562";
        parentPlace.name = "Some Parent Group";
        parentPlace.type = "group";
        parentPlace.uri = "https://jive.com/api/core/v3/places/61562";
        jiveContent.parentPlace = parentPlace;

        Map<String, JiveRemote.Link> resources = new HashMap<>();
        resources.put("html", link("https://jive.com/docs/DOC-1072237", Arrays.asList("GET")));
        resources.put("extprops", link("https://jive.com/api/core/v3/contents/1278973/extprops", Arrays.asList("POST", "DELETE", "GET")));
        jiveContent.resources = resources;

        JiveRemote.PagedJiveContent pagedContent = new JiveRemote.PagedJiveContent();
        pagedContent.itemsPerPage = 1;
        pagedContent.links.put("next", "https://jive.com/api/core/v3/contents?sort=dateCreatedDesc&fields=id,contentID,tags,updated,published,parentPlace,subject,resources,content,via,parent&filter=entityDescriptor%28102,1072237%29&abridged=false&includeBlogs=false&count=1&startIndex=1");
        pagedContent.startIndex = 0;
        pagedContent.list = Arrays.asList(jiveContent);
        return pagedContent;
    }

    private JiveRemote.Link link(String ref, List<String> allowed){
        JiveRemote.Link link = new JiveRemote.Link();
        link.ref = ref;
        link.allowed = allowed;
        return link;
    }
}
