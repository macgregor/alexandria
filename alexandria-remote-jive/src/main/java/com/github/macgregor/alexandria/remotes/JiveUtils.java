package com.github.macgregor.alexandria.remotes;

import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Context;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for working with a Jive remote api
 */
public class JiveUtils {

    /**
     * Extract the pseudo-identifier used in the {@link Config.DocumentMetadata#remoteUri}
     * for use in a search request to retrieve the actual {@value JiveRemote#JIVE_CONTENT_ID}.
     *
     * @param remoteDoc  {@link Config.DocumentMetadata#remoteUri}
     * @return  jive object id extracted from the uri
     */
    protected static String jiveObjectId(URI remoteDoc){
        Pattern p = Pattern.compile(".*DOC-(\\d+)-*.*");
        Matcher m = p.matcher(remoteDoc.getPath());
        if(m.matches()) {
            return m.group(1);
        } else {
            throw new IllegalStateException(String.format("Unable to extract jive object id from %s.", remoteDoc.toString()));
        }
    }

    /**
     * Extract the parent place name from the user defined {@value JiveRemote#JIVE_PARENT_URI} for use in a search request for the
     * actual {@value JiveRemote#JIVE_PARENT_API_URI}.
     *
     * @param parentPlaceUrl  {@value JiveRemote#JIVE_PARENT_URI} value from {@link Config.DocumentMetadata#extraProps}
     * @return  the extracted parent place name
     */
    protected static String jiveParentPlaceName(String parentPlaceUrl){
        Pattern p = Pattern.compile(".*/(.*)");
        Matcher m = p.matcher(parentPlaceUrl);
        if(m.matches()) {
            return m.group(1);
        } else {
            throw new IllegalStateException(String.format("Unable to parent place name from %s.", parentPlaceUrl));
        }
    }

    /**
     * Set a UUID tracking tag Alexandria can use to track down documents.
     *
     * Rest APIs are fickle things. They may or may not provide decent search apis or reliable methods
     * that return data needed to identify created or updated documents. Adding this tag lets us quickly
     * and easily search for documents if we cant use the Jive identifier for some reason.
     *
     * @param context  current Alexandria context
     * @param metadata  document metadata to add tracking tag to
     */
    protected static void setTrackingTagAsNeeded(Context context, Config.DocumentMetadata metadata){
        if(metadata.hasExtraProperty(JiveRemote.JIVE_TRACKING_TAG)){
            return;
        }
        metadata.setExtraProperty(JiveRemote.JIVE_TRACKING_TAG, UUID.randomUUID().toString());
    }

    /**
     * Set tags for the document resolving default tags, document tags and remote specific tags.
     *
     * @see Context#getTagsForDocument(Config.DocumentMetadata)
     *
     * @param context  Current Alexandria context
     * @param metadata  document metadata to get tags for
     * @return  list of tags to add to the request or empty list if none are set
     */
    protected static List<String> getTagsForDocument(Context context, Config.DocumentMetadata metadata){
        List<String> tags = context.getTagsForDocument(metadata);
        if(metadata.hasExtraProperty(JiveRemote.JIVE_TRACKING_TAG)) {
            tags.add(metadata.getExtraProperty(JiveRemote.JIVE_TRACKING_TAG));
        }
        return tags;
    }

    /**
     * Determine if the indexed document needs to fetch the {@value JiveRemote#JIVE_CONTENT_ID} from the remote.
     *
     * @param metadata  document to check
     * @return  true if {@value JiveRemote#JIVE_CONTENT_ID} needs to be retrieved from remote, false if its already set.
     */
    protected static boolean needsContentId(Config.DocumentMetadata metadata){
        if(metadata.remoteUri().isPresent()){
            if(metadata.extraProps().isPresent()){
                return !metadata.extraProps().get().containsKey(JiveRemote.JIVE_CONTENT_ID);
            }
            return true;
        }
        return false;
    }

    /**
     * Determine if an indexed document has a parent ({@value JiveRemote#JIVE_PARENT_URI} but needs to have the parent place id looked up.
     *
     * @param metadata  document to check for parent information
     * @return  true if document has a parent but no {@value JiveRemote#JIVE_PARENT_API_URI}, false if no parent or {@value JiveRemote#JIVE_PARENT_API_URI} already set
     */
    public static boolean needsParentPlaceUri(Context context, Config.DocumentMetadata metadata){
        return context.getExtraPropertiesForDocument(metadata).containsKey(JiveRemote.JIVE_PARENT_URI) &&
                !context.getExtraPropertiesForDocument(metadata).containsKey(JiveRemote.JIVE_PARENT_API_URI);
    }
}
