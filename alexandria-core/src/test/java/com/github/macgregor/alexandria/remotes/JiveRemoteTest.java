package com.github.macgregor.alexandria.remotes;

import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Config.RemoteConfig;
import com.github.macgregor.alexandria.Jackson;
import com.github.macgregor.alexandria.Resources;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class JiveRemoteTest {

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

        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("DOC-1072237.md"));
        metadata.remoteUri(Optional.of(new URI("https://jive.com/docs/DOC-1072237")));

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.syncMetadata(metadata))
                .withMessageContaining("Bad request");
    }

    @Test
    public void testSyncMetadata500() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setResponseCode(500));

        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("DOC-1072237.md"));
        metadata.remoteUri(Optional.of(new URI("https://jive.com/docs/DOC-1072237")));

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.syncMetadata(metadata))
                .withMessageContaining("Unexpected status code");
    }

    @Test
    public void testSyncMetadataUpdatesMetadataFromResponse() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237-Paged.json")));

        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("DOC-1072237.md"));
        metadata.remoteUri(Optional.of(new URI("https://jive.com/docs/DOC-1072237")));
        jiveRemote.syncMetadata(metadata);

        assertThat(metadata.lastUpdated().get())
                .isEqualTo(ZonedDateTime.parse("2018-06-22T18:42:59.652+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.createdOn().get())
                .isEqualTo(ZonedDateTime.parse("2016-03-21T15:07:34.533+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.extraProps().get().get("jiveParentUrl")).isEqualTo("https://jive.com/groups/parent_group");
        assertThat(metadata.extraProps().get().get("jiveParentPlaceId")).isEqualTo("61562");
        assertThat(metadata.extraProps().get().get("jiveContentId")).isEqualTo("1278973");
    }

    @Test
    public void testCreateUpdatesMetadataFromResponse() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json")));

        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("DOC-1072237.md"));
        metadata.remoteUri(Optional.of(new URI("https://jive.com/docs/DOC-1072237")));
        metadata.convertedPath(Optional.of(folder.newFile().toPath()));
        jiveRemote.create(metadata);

        assertThat(metadata.lastUpdated().get())
                .isEqualTo(ZonedDateTime.parse("2018-06-22T18:42:59.652+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.createdOn().get())
                .isEqualTo(ZonedDateTime.parse("2016-03-21T15:07:34.533+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.extraProps().get().get("jiveParentUrl")).isEqualTo("https://jive.com/groups/parent_group");
        assertThat(metadata.extraProps().get().get("jiveParentPlaceId")).isEqualTo("61562");
        assertThat(metadata.extraProps().get().get("jiveContentId")).isEqualTo("1278973");
    }

    @Test
    public void testCreate400() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setResponseCode(400));

        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("DOC-1072237.md"));
        metadata.remoteUri(Optional.of(new URI("https://jive.com/docs/DOC-1072237")));
        metadata.convertedPath(Optional.of(folder.newFile().toPath()));

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.create(metadata))
                .withMessageContaining("Bad request");
    }

    @Test
    public void testCreate409() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setResponseCode(409));

        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("DOC-1072237.md"));
        metadata.remoteUri(Optional.of(new URI("https://jive.com/docs/DOC-1072237")));
        metadata.convertedPath(Optional.of(folder.newFile().toPath()));

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.create(metadata))
                .withMessageContaining("Document conflicts with existing document");
    }

    @Test
    public void testCreate403() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setResponseCode(403));

        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("DOC-1072237.md"));
        metadata.remoteUri(Optional.of(new URI("https://jive.com/docs/DOC-1072237")));
        metadata.convertedPath(Optional.of(folder.newFile().toPath()));

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.create(metadata))
                .withMessageContaining("Unauthorized to access document");
    }

    @Test
    public void testUpdateUpdatesMetadataFromResponse() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237-Paged.json")),
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237.json"))));

        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("DOC-1072237.md"));
        metadata.remoteUri(Optional.of(new URI("https://jive.com/docs/DOC-1072237")));
        metadata.convertedPath(Optional.of(folder.newFile().toPath()));
        jiveRemote.update(metadata);

        assertThat(metadata.lastUpdated().get())
                .isEqualTo(ZonedDateTime.parse("2018-06-22T18:42:59.652+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.createdOn().get())
                .isEqualTo(ZonedDateTime.parse("2016-03-21T15:07:34.533+0000", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ")));
        assertThat(metadata.extraProps().get().get("jiveParentUrl")).isEqualTo("https://jive.com/groups/parent_group");
        assertThat(metadata.extraProps().get().get("jiveParentPlaceId")).isEqualTo("61562");
        assertThat(metadata.extraProps().get().get("jiveContentId")).isEqualTo("1278973");
    }

    @Test
    public void testUpdate400() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setResponseCode(400));

        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("DOC-1072237.md"));
        metadata.remoteUri(Optional.of(new URI("https://jive.com/docs/DOC-1072237")));
        metadata.convertedPath(Optional.of(folder.newFile().toPath()));

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.update(metadata))
                .withMessageContaining("Bad request");
    }

    @Test
    public void testUpdate409() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setResponseCode(409));

        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("DOC-1072237.md"));
        metadata.remoteUri(Optional.of(new URI("https://jive.com/docs/DOC-1072237")));
        metadata.convertedPath(Optional.of(folder.newFile().toPath()));

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.update(metadata))
                .withMessageContaining("Document conflicts with existing document");
    }

    @Test
    public void testUpdate403() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setResponseCode(403));

        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("DOC-1072237.md"));
        metadata.remoteUri(Optional.of(new URI("https://jive.com/docs/DOC-1072237")));
        metadata.convertedPath(Optional.of(folder.newFile().toPath()));

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.update(metadata))
                .withMessageContaining("Unauthorized to access document");
    }

    @Test
    public void testUpdate404() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setResponseCode(404));

        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("DOC-1072237.md"));
        metadata.remoteUri(Optional.of(new URI("https://jive.com/docs/DOC-1072237")));
        metadata.convertedPath(Optional.of(folder.newFile().toPath()));

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.update(metadata))
                .withMessageContaining("Document doesnt exist");
    }

    @Test
    public void testDeleteSetsDeletedDateTime() throws URISyntaxException, IOException {
        JiveRemote jiveRemote = setup(Arrays.asList(
                new MockResponse().setBody(Resources.load("src/test/resources/DOC-1072237-Paged.json")),
                new MockResponse().setResponseCode(204)));

        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("DOC-1072237.md"));
        metadata.remoteUri(Optional.of(new URI("https://jive.com/docs/DOC-1072237")));
        jiveRemote.delete(metadata);
        assertThat(metadata.deletedOn()).isPresent();
    }

    @Test
    public void testDelete400() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setResponseCode(400));

        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("DOC-1072237.md"));
        metadata.remoteUri(Optional.of(new URI("https://jive.com/docs/DOC-1072237")));

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.delete(metadata))
                .withMessageContaining("Bad request");
    }

    @Test
    public void testDelete403() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setResponseCode(403));

        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("DOC-1072237.md"));
        metadata.remoteUri(Optional.of(new URI("https://jive.com/docs/DOC-1072237")));

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.delete(metadata))
                .withMessageContaining("Unauthorized to access document");
    }

    @Test
    public void testDelete404() throws IOException, URISyntaxException {
        JiveRemote jiveRemote = setup(new MockResponse().setResponseCode(404));

        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.sourcePath(Paths.get("DOC-1072237.md"));
        metadata.remoteUri(Optional.of(new URI("https://jive.com/docs/DOC-1072237")));

        assertThatExceptionOfType(HttpException.class)
                .isThrownBy(() -> jiveRemote.delete(metadata))
                .withMessageContaining("Document doesnt exist");
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
        config.baseUrl(baseUrl.toString());
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

        Map<String, JiveRemote.JiveContent.Link> resources = new HashMap<>();
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

    private JiveRemote.JiveContent.Link link(String ref, List<String> allowed){
        JiveRemote.JiveContent.Link link = new JiveRemote.JiveContent.Link();
        link.ref = ref;
        link.allowed = allowed;
        return link;
    }
}
