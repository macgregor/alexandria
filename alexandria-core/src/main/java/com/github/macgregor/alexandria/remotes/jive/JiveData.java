package com.github.macgregor.alexandria.remotes.jive;

import lombok.EqualsAndHashCode;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JiveData{

    @EqualsAndHashCode
    public static class Link{
        public String ref;
        public List<String> allowed;
    }

    @EqualsAndHashCode
    public static class PagedJivePlace{
        public List<JivePlace> list = new ArrayList<>();
        public Integer startIndex;
        public Integer itemsPerPage;
    }

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

    @EqualsAndHashCode
    public static class PagedJiveContent{
        public Map<String, String> links = new HashMap<>();
        public List<JiveContent> list = new ArrayList<>();
        public Integer startIndex;
        public Integer itemsPerPage;
    }

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
