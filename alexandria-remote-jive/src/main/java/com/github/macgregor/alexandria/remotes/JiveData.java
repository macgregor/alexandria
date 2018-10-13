package com.github.macgregor.alexandria.remotes;

import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Context;
import com.github.macgregor.alexandria.Resources;
import lombok.EqualsAndHashCode;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Jive specific POJOs for making requests and parsing responses.
 */
public class JiveData{

    /**
     * Create the post body for a create or update request from the given {@link Config.DocumentMetadata}.
     *
     * @see <a href="https://developers.jivesoftware.com/api/v3/cloud/rest/DocumentEntity.html">Jive REST API - Document Entity</a>
     *
     * @param context  current Alexandria context
     * @param metadata  document metadata to generate post body from
     * @return  String representation of the json structure for the request
     * @throws IOException  the converted document couldnt be loaded
     */
    protected static JiveContent documentPostBody(Context context, Config.DocumentMetadata metadata) throws IOException {
        JiveData.JiveContent jiveDocument = new JiveData.JiveContent();
        jiveDocument.parentPlace = null; //parent place is only in responses
        jiveDocument.subject = metadata.title();
        jiveDocument.content.text = Resources.load(context.convertedPath(metadata).get().toString());
        jiveDocument.type = "document";
        jiveDocument.typeCode = 102;

        if(metadata.extraProps().get().containsKey(JiveRemote.JIVE_CONTENT_ID)){
            jiveDocument.contentID = metadata.extraProps().get().get(JiveRemote.JIVE_CONTENT_ID);
        }

        if(metadata.extraProps().get().containsKey(JiveRemote.JIVE_PARENT_API_URI)) {
            jiveDocument.parent = metadata.extraProps().get().get(JiveRemote.JIVE_PARENT_API_URI);
        }

        jiveDocument.tags = JiveUtils.getTagsForDocument(context, metadata);
        return jiveDocument;
    }

    /**
     * Represents a link url and what methods can be performed on it
     */
    @EqualsAndHashCode
    public static class Link {
        public String ref;
        public List<String> allowed;
    }

    /**
     * Represents a jive place
     *
     * @see <a href="https://developers.jivesoftware.com/api/v3/cloud/rest/PlaceEntity.html">Jive REST API - Place</a>
     */
    @EqualsAndHashCode
    public static class JivePlace{
        public static final String FIELDS = "id,resources,placeID,displayName,name,type,typeCode";
        public Integer id;
        public Map<String, Link> resources = new HashMap<>();
        public String placeID;
        public String displayName;
        public String name;
        public String type;
        public Integer typeCode;
    }

    /**
     * Represents Jive content, currently only supports documents.
     *
     * @see <a href="https://developers.jivesoftware.com/api/v3/cloud/rest/DocumentEntity.html">Jive REST API - Document Entity</a>
     */
    @EqualsAndHashCode
    public static class JiveContent {
        public static final String FIELDS = "id,contentID,tags,updated,published,parentPlace,subject,resources,content,via,parent";
        public Integer id;
        public String contentID;
        public ZonedDateTime published;
        public ZonedDateTime updated;
        public List<String> tags = new ArrayList<>();
        public String type; // "document" for documents
        public Integer typeCode; // 102 for documents
        public String subject; //document name
        public JiveContent.Content content = new JiveContent.Content();
        public JiveContent.Via via = new JiveContent.Via();
        public Map<String, Link> resources = new HashMap<>();
        public String parent;
        public JiveContent.ParentPlace parentPlace;

        @EqualsAndHashCode
        public static class Content {
            public String type;
            public String text;
            public Boolean editable;
        }

        @EqualsAndHashCode
        public static class Via {
            public final String displayName = "Alexandria";
            public final String url = "https://github.com/macgregor/alexandria";
        }

        @EqualsAndHashCode
        public static class ParentPlace {
            public Integer id;
            public String html;
            public String placeID;
            public String name;
            public String type;
            public String uri;
        }
    }
}
