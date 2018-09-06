package com.github.macgregor.alexandria.remotes.jive;

import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Context;
import com.github.macgregor.alexandria.TestData;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JiveUtilsTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    public Context context;
    public Config.DocumentMetadata metadata;

    @Before
    public void setup() throws IOException {
        context = TestData.minimalContext(folder);
        metadata = context.config().metadata().get().get(0);
    }

    @Test
    public void testJiveObjectIdThrowsIllegalArgumentOnBadPattern(){
        assertThatThrownBy(() -> JiveUtils.jiveObjectId(new URI("asldkasd"))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testJiveParentPlaceNameThrowsIllegalArgumentOnBadPattern(){
        assertThatThrownBy(() -> JiveUtils.jiveParentPlaceName("asldkasd")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testJiveDoesntNeedContentIdWhenNoRemoteUri(){
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.remoteUri(Optional.empty());
        assertThat(JiveUtils.needsContentId(metadata)).isFalse();
    }

    @Test
    public void testJiveNeedsContentIdWhenRemoteUriWithoutExtraProps() throws URISyntaxException {
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.remoteUri(Optional.of(new URI("foo")));
        metadata.extraProps(Optional.empty());
        assertThat(JiveUtils.needsContentId(metadata)).isTrue();
    }

    @Test
    public void testJiveNeedsContentIdWhenRemoteUriWithoutJiveContentIdProperty() throws URISyntaxException {
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.remoteUri(Optional.of(new URI("foo")));
        metadata.extraProps(Optional.of(Collections.emptyMap()));
        assertThat(JiveUtils.needsContentId(metadata)).isTrue();
    }

    @Test
    public void testJiveDoesntNeedContentIdWhenRemoteUriWithJiveContentIdProperty() throws URISyntaxException {
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.remoteUri(Optional.of(new URI("foo")));
        metadata.extraProps(Optional.of(Collections.singletonMap("jiveContentId", "1234")));
        assertThat(JiveUtils.needsContentId(metadata)).isFalse();
    }

    @Test
    public void testJiveDoesntNeedParentPlaceUriWhenNoExtraProps(){
        Context context = new Context();
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.extraProps(Optional.empty());
        assertThat(JiveUtils.needsParentPlaceUri(context, metadata)).isFalse();
    }

    @Test
    public void testJiveNeedsParentPlaceUriWhenNoJiveParentApiUriIsPresent(){
        Context context = new Context();
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.extraProps(Optional.of(Collections.singletonMap(JiveRemote.JIVE_PARENT_URI, "foo")));
        assertThat(JiveUtils.needsParentPlaceUri(context, metadata)).isTrue();
    }

    @Test
    public void testJiveDoesntNeedParentPlaceUriWhenJiveParentApiUriIsPresent(){
        Context context = new Context();
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        Map<String, String> extraProps = new HashMap<>();
        extraProps.put(JiveRemote.JIVE_PARENT_API_URI, "foo");
        extraProps.put(JiveRemote.JIVE_PARENT_PLACE_ID, "foo");
        metadata.extraProps(Optional.of(extraProps));
        assertThat(JiveUtils.needsParentPlaceUri(context, metadata)).isFalse();
    }

    @Test
    public void testJiveInheritsDefaultParentUri(){
        Context context = new Context();
        context.config().remote().defaultExtraProps(Optional.of(Collections.singletonMap(JiveRemote.JIVE_PARENT_URI, "foo")));
        Config.DocumentMetadata metadata = new Config.DocumentMetadata();
        metadata.extraProps(Optional.empty());
        assertThat(JiveUtils.needsParentPlaceUri(context, metadata)).isTrue();
    }

    @Test
    public void trackingTagSetIfMissing(){
        JiveUtils.setTrackingTagAsNeeded(context, metadata);
        assertThat(metadata.hasExtraProperty(JiveRemote.JIVE_TRACKING_TAG)).isTrue();
    }

    @Test
    public void trackingTagNotOverridden(){
        metadata.setExtraProperty(JiveRemote.JIVE_TRACKING_TAG, "foo");
        JiveUtils.setTrackingTagAsNeeded(context, metadata);
        assertThat(metadata.getExtraProperty(JiveRemote.JIVE_TRACKING_TAG)).isEqualTo("foo");
    }

    @Test
    public void documentTagsIncludeTrackingTag(){
        metadata.setExtraProperty(JiveRemote.JIVE_TRACKING_TAG, UUID.randomUUID().toString());
        assertThat(JiveUtils.getTagsForDocument(context, metadata)).contains(metadata.getExtraProperty(JiveRemote.JIVE_TRACKING_TAG));
    }

    @Test
    public void documentTagsIncludeDefaultTags(){
        context.config().defaultTags(Optional.of(Collections.singletonList("default")));
        assertThat(JiveUtils.getTagsForDocument(context, metadata)).contains("default");
    }

    @Test
    public void documentTagsIncludeDocumentTags(){
        metadata.tags(Optional.of(Collections.singletonList("document")));
        assertThat(JiveUtils.getTagsForDocument(context, metadata)).contains("document");
    }

    @Test
    public void documentTagsEmptyListWhenNoTagsAnywhere(){
        assertThat(JiveUtils.getTagsForDocument(context, metadata)).isEmpty();
    }
}
