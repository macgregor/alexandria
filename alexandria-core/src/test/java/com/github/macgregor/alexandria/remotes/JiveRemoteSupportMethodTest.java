package com.github.macgregor.alexandria.remotes;

import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Context;
import com.github.macgregor.alexandria.TestData;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class JiveRemoteSupportMethodTest {

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
    public void trackingTagSetIfMissing(){
        JiveRemote.setTrackingTagAsNeeded(context, metadata);
        assertThat(metadata.hasExtraProperty(JiveRemote.JIVE_TRACKING_TAG)).isTrue();
    }

    @Test
    public void trackingTagNotOverridden(){
        metadata.setExtraProperty(JiveRemote.JIVE_TRACKING_TAG, "foo");
        JiveRemote.setTrackingTagAsNeeded(context, metadata);
        assertThat(metadata.getExtraProperty(JiveRemote.JIVE_TRACKING_TAG)).isEqualTo("foo");
    }

    @Test
    public void documentTagsIncludeTrackingTag(){
        metadata.setExtraProperty(JiveRemote.JIVE_TRACKING_TAG, UUID.randomUUID().toString());
        assertThat(JiveRemote.getTagsForDocument(context, metadata)).contains(metadata.getExtraProperty(JiveRemote.JIVE_TRACKING_TAG));
    }

    @Test
    public void documentTagsIncludeDefaultTags(){
        context.config().defaultTags(Optional.of(Collections.singletonList("default")));
        assertThat(JiveRemote.getTagsForDocument(context, metadata)).contains("default");
    }

    @Test
    public void documentTagsIncludeDocumentTags(){
        metadata.tags(Optional.of(Collections.singletonList("document")));
        assertThat(JiveRemote.getTagsForDocument(context, metadata)).contains("document");
    }

    @Test
    public void documentTagsEmptyListWhenNoTagsAnywhere(){
        assertThat(JiveRemote.getTagsForDocument(context, metadata)).isEmpty();
    }
}
