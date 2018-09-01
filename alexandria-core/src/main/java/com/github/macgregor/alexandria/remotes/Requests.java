package com.github.macgregor.alexandria.remotes;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okio.Buffer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Requests {

    protected static Request.Builder requestBuilder(HttpUrl route, Map<String, String> headers){
        return addHeaders(new Request.Builder().url(route), headers);
    }

    protected static HttpUrl.Builder routeBuilder(String baseUrl, List<String> segments, Map<String, String> queryParameters){
        HttpUrl.Builder builder = HttpUrl.parse(baseUrl).newBuilder();
        addSegments(builder, segments);
        addQueryParameters(builder, queryParameters);
        return builder;
    }

    protected static HttpUrl.Builder addQueryParameters(HttpUrl.Builder builder, Map<String, String> queryParameters){
        if(queryParameters != null){
            for(Map.Entry<String, String> entry : queryParameters.entrySet()){
                builder.addQueryParameter(entry.getKey(), entry.getValue());
            }
        }
        return builder;
    }

    protected static HttpUrl.Builder addSegments(HttpUrl.Builder builder, List<String> segments){
        if(segments != null){
            for(String segment : segments){
                builder.addPathSegment(segment);
            }
        }
        return builder;
    }

    protected static Request.Builder addHeaders(Request.Builder requestBuilder, Map<String, String> headers){
        if(headers != null){
            for(Map.Entry<String, String> entry : headers.entrySet()){
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return requestBuilder;
    }

    protected static String bodyToString(final Request request) throws IOException {
        final Request copy = request.newBuilder().build();
        final Buffer buffer = new Buffer();
        copy.body().writeTo(buffer);
        return buffer.readUtf8();
    }
}
