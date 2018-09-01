package com.github.macgregor.alexandria.remotes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.macgregor.alexandria.Jackson;
import com.github.macgregor.alexandria.exceptions.HttpException;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter @Setter @Accessors(fluent = true)
@Builder
public class RemoteDocument<T> {
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @NonNull private String baseUrl;
    @NonNull private Class<T> entity;
    @Builder.Default private Integer requestTimeout = 30;
    @Singular private Map<String, String> headers;
    @Singular private Map<String, String> queryParameters;
    @Singular private List<String> pathSegments;
    @Singular private List<Integer> allowableStatusCodes;

    public T get() throws HttpException {
        Request request = Requests.requestBuilder(route(), headers).get().build();

        try {
            Response response = doRequest(request);
            return parseResponse(response);
        } catch(HttpException e){
            e.request(Optional.of(request));
            throw e;
        }
    }

    public List<T> get(int offset, int count){
        return null;
    }

    public T put(T t) throws HttpException {
        Request request = null;
        try {
            request = Requests.requestBuilder(route(), headers)
                    .put(requestBody(t))
                    .build();
            Response response = doRequest(request);
            return parseResponse(response);
        } catch(HttpException e){
            e.request(Optional.of(request));
            throw e;
        }
    }

    public T post(T t) throws HttpException {
        Request request = null;
        try {
            request = Requests.requestBuilder(route(), headers)
                    .post(requestBody(t))
                    .build();
            Response response = doRequest(request);
            return parseResponse(response);
        } catch(HttpException e){
            e.request(Optional.of(request));
            throw e;
        }

    }

    public void delete() throws HttpException {
        Request request = Requests.requestBuilder(route(), headers).delete().build();

        try {
           doRequest(request);
        } catch(HttpException e){
            e.request(Optional.of(request));
            throw e;
        }
    }

    protected RequestBody requestBody(T t) throws HttpException {
        try {
            return RequestBody.create(JSON, Jackson.jsonMapper().writeValueAsString(t));
        } catch (JsonProcessingException e) {
            throw new HttpException.Builder()
                    .withMessage("Unable to parse content")
                    .causedBy(e)
                    .build();
        }
    }

    protected T parseResponse(Response response) throws HttpException {
        try {
            return Jackson.jsonMapper().readValue(response.body().charStream(), entity);
        } catch (IOException e) {
            log.warn("Cannot parse response content", e);
            throw new HttpException.Builder()
                    .withMessage("Cannot parse response content")
                    .responseContext(response)
                    .causedBy(e)
                    .build();
        }
    }

    protected HttpUrl route(){
        return Requests.routeBuilder(baseUrl, pathSegments, queryParameters).build();
    }

    protected OkHttpClient client(){
        return new OkHttpClient.Builder()
                .connectTimeout(requestTimeout, TimeUnit.SECONDS)
                .writeTimeout(requestTimeout, TimeUnit.SECONDS)
                .readTimeout(requestTimeout, TimeUnit.SECONDS)
                .build();
    }

    protected Response doRequest(Request request) throws HttpException {
        log.debug(request.toString());
        if(request.body() != null) {
            try {
                log.debug(Requests.bodyToString(request));
            } catch (IOException e) {}
        }

        Call call = client().newCall(request);

        Response response = null;
        try {
            response = call.execute();
            log.debug(response.toString());
        } catch (IOException e) {
            log.debug("Request error", e);
            throw new HttpException.Builder()
                    .withMessage(String.format("Unable to make request %s %s", request.method(), request.url().toString()))
                    .causedBy(e)
                    .requestContext(request)
                    .responseContext(response)
                    .build();
        }
        
        if((allowableStatusCodes.isEmpty() && response.isSuccessful()) ||
                allowableStatusCodes.contains(response.code())){
            return response;
        } else{
            String expected = allowableStatusCodes.isEmpty() ? "20X" : StringUtils.join(allowableStatusCodes);
            throw new HttpException.Builder()
                    .withMessage(String.format("%s %s - %d (excepted one of [%s])",
                            request.method(), request.url().toString(), response.code(), expected))
                    .responseContext(response)
                    .requestContext(request)
                    .build();
        }
    }
}
