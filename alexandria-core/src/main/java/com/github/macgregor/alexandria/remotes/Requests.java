package com.github.macgregor.alexandria.remotes;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okio.Buffer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Utility class for making rest requests
 */
public class Requests {

    /**
     * Create a {@link Request.Builder} from the provided route, adding all headers
     *
     * @param route  {@link HttpUrl} the request should be made to
     * @param headers  optional headers to add to the request
     * @return  {@link Request.Builder}
     */
    public static Request.Builder requestBuilder(HttpUrl route, Map<String, String> headers){
        return addHeaders(new Request.Builder().url(route), headers);
    }

    /**
     * Create a {@link HttpUrl.Builder} from the baseUrl, path segments and query parameters.
     *
     * Path segments will be added to the url path in the order they given. So
     * {@code routeBuilder("www.google.com", Arrays.asList("foo", "bar"), Collections.emptyMap())} would build a request
     * url www.googl.com/foo/bar
     *
     * @param baseUrl  base url for the request
     * @param segments  optional path segments to add to the request url
     * @param queryParameters  optional query parameters to add to the request url
     * @return  {@link HttpUrl.Builder}
     */
    public static HttpUrl.Builder routeBuilder(String baseUrl, List<String> segments, Map<String, String> queryParameters){
        HttpUrl.Builder builder = HttpUrl.parse(baseUrl).newBuilder();
        addSegments(builder, segments);
        addQueryParameters(builder, queryParameters);
        return builder;
    }

    /**
     * Add query parameters to the {@link HttpUrl.Builder}
     *
     * @param builder  existing {@link HttpUrl.Builder} to add query parameters to
     * @param queryParameters  query parameters to add
     * @return  {@link HttpUrl.Builder}
     */
    public static HttpUrl.Builder addQueryParameters(HttpUrl.Builder builder, Map<String, String> queryParameters){
        if(queryParameters != null){
            for(Map.Entry<String, String> entry : queryParameters.entrySet()){
                builder.addQueryParameter(entry.getKey(), entry.getValue());
            }
        }
        return builder;
    }

    /**
     * Add url segments to a {@link HttpUrl.Builder}
     *
     * @param builder  existing {@link HttpUrl.Builder} to add url segments to
     * @param segments  url segments to add
     * @return  {@link HttpUrl.Builder}
     */
    public static HttpUrl.Builder addSegments(HttpUrl.Builder builder, List<String> segments){
        if(segments != null){
            for(String segment : segments){
                builder.addPathSegment(segment);
            }
        }
        return builder;
    }

    /**
     * Add headers to a {@link Request.Builder}
     *
     * @param requestBuilder  existing {@link Request.Builder} to add headers to
     * @param headers  headers to add
     * @return  {@link Request.Builder}
     */
    public static Request.Builder addHeaders(Request.Builder requestBuilder, Map<String, String> headers){
        if(headers != null){
            for(Map.Entry<String, String> entry : headers.entrySet()){
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return requestBuilder;
    }

    /**
     * Return the request body in string form.
     *
     * @param request  {@link Request} with a body to stringify
     * @return  String representation of the request body
     * @throws IOException
     */
    public static String bodyToString(final Request request) throws IOException {
        final Request copy = request.newBuilder().build();
        final Buffer buffer = new Buffer();
        copy.body().writeTo(buffer);
        return buffer.readUtf8();
    }
}
