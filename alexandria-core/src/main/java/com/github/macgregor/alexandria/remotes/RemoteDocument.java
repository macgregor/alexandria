package com.github.macgregor.alexandria.remotes;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.macgregor.alexandria.Jackson;
import com.github.macgregor.alexandria.exceptions.HttpException;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Abstraction around a remote document format to make get/put/post/delete methods against a rest api.
 *
 * The class is generic, taking a POJO that makes up the request body for put/post and the response for gets.
 * The class handles error handling and marshalling for you, leaving less boiler plate in the actual application logic.
 *
 * <pre>
 * {@code
 * class Foo{
 *     Integer id;
 *     String name;
 *     String address;
 * }
 *
 * RemoteDocument<Foo> remoteFoo = RemoteDocument.<Foo>builder()
 *          .baseUrl("www.google.com")
 *          .pathSegment("foo")
 *          .pathSegment("1")
 *          .header("Authorization", Credentials.basic("username", "password"))
 *          .queryParameter("fields", "id,name,address")
 *          .build();
 *
 * Foo foo = remoteFoo.get();
 * }
 * </pre>
 *
 * @param <T>
 */
@Slf4j
@Getter @Setter @Accessors(fluent = true)
@Builder(toBuilder = true)
public class RemoteDocument<T>{
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @NonNull private String baseUrl;
    @NonNull private Class<T> entity;
    @Builder.Default private Integer requestTimeout = 30;
    @Singular private Map<String, String> headers;
    @Singular private Map<String, String> queryParameters;
    @Singular private List<String> pathSegments;
    @Singular private List<Integer> allowableStatusCodes;
    @Builder.Default private String pageOffsetRequestParameter = "startIndex";
    @Builder.Default private String pageSizeRequestParameter = "count";
    @Builder.Default private String pageOffsetResponseField = "startIndex";
    @Builder.Default private String pageSizeResponseField = "itemsPerPage";
    @Builder.Default private String pageListResponseField = "list";

    public T get() throws HttpException {
        Request request = null;

        try {
            request = Requests.requestBuilder(route(), headers).get().build();
            Response response = doRequest(request);
            return parseResponse(response);
        } catch(HttpException e){
            e.request(Optional.of(request));
            throw e;
        } catch(Exception e){
            throw new HttpException.Builder()
                    .withMessage("Unexpected error executing GET")
                    .causedBy(e)
                    .requestContext(request)
                    .build();
        }
    }

    public RemoteDocumentPage<T> getPaged(){
        return new RemoteDocumentPage(this.toBuilder());
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
        } catch(Exception e){
            throw new HttpException.Builder()
                    .withMessage("Unexpected error executing PUT")
                    .causedBy(e)
                    .requestContext(request)
                    .build();
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
        } catch(Exception e){
            throw new HttpException.Builder()
                    .withMessage("Unexpected error executing POST")
                    .causedBy(e)
                    .requestContext(request)
                    .build();
        }

    }

    public void delete() throws HttpException {
        Request request = null;
        try {
            request = Requests.requestBuilder(route(), headers).delete().build();
            doRequest(request);
        } catch(HttpException e){
            e.request(Optional.ofNullable(request));
            throw e;
        } catch(Exception e){
            throw new HttpException.Builder()
                    .withMessage("Unexpected error executing DELETE")
                    .causedBy(e)
                    .requestContext(request)
                    .build();
        }
    }

    protected RequestBody requestBody(T t) throws HttpException {
        try {
            return RequestBody.create(JSON, Jackson.jsonMapper().writeValueAsString(t));
        } catch (Exception e) {
            throw new HttpException.Builder()
                    .withMessage("Unable to parse content")
                    .causedBy(e)
                    .build();
        }
    }

    protected T parseResponse(Response response) throws HttpException {
        try {
            return Jackson.jsonMapper().readValue(response.body().charStream(), entity);
        } catch (Exception e) {
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
            String expected = allowableStatusCodes.isEmpty() ? "20X" : StringUtils.join(allowableStatusCodes, ",");
            throw new HttpException.Builder()
                    .withMessage(String.format("%s %s - %d (excepted one of [%s])",
                            request.method(), request.url().toString(), response.code(), expected))
                    .responseContext(response)
                    .requestContext(request)
                    .build();
        }
    }

    public static class RemoteDocumentPage<T> implements Iterable<T> {
        private RemoteDocumentBuilder requestBuilder;

        protected RemoteDocumentPage(RemoteDocumentBuilder requestBuilder){
            this.requestBuilder = requestBuilder;
        }

        @Override
        public Iterator<T> iterator() {
            return new RemoteDocumentIterator(requestBuilder);
        }

        public T first() throws HttpException {
            Iterator<T> iterator = iterator();
            try{
                if(iterator.hasNext()){
                    return iterator.next();
                } else{
                    return null;
                }
            } catch(Exception e){
                if(e.getCause() instanceof HttpException){
                    throw (HttpException)e.getCause();
                }
                throw e;
            }
        }
    }

    static class RemoteDocumentIterator<T> implements Iterator<T>{

        private RemoteDocumentBuilder requestBuilder;
        private Iterator<T> current;
        private Integer pageSize = 25;
        private Integer offset = 0;
        private boolean finished = false;

        protected RemoteDocumentIterator(RemoteDocumentBuilder requestBuilder){
            this.requestBuilder = requestBuilder;
        }

        @Override
        public boolean hasNext() {
            if(current == null || (!current.hasNext() && !finished)){
                current = nextPage();
            }
            return current.hasNext();
        }

        @Override
        public T next() {
            return current.next();
        }

        protected Iterator<T> nextPage() {
            RemoteDocument remoteDocument = requestBuilder
                    .queryParameter(requestBuilder.pageSizeRequestParameter, pageSize.toString())
                    .queryParameter(requestBuilder.pageOffsetRequestParameter, offset.toString())
                    .build();
            Request request = null;
            try {
                request = Requests.requestBuilder(remoteDocument.route(), remoteDocument.headers).get().build();
                Response response = remoteDocument.doRequest(request);

                JsonFactory factory = new JsonFactory();
                ObjectMapper mapper = new ObjectMapper(factory);
                JsonNode rootNode = mapper.readTree(response.body().charStream());
                JsonNode results = rootNode.get(remoteDocument.pageListResponseField);
                JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, remoteDocument.entity);
                List<T> parsedResults = Jackson.jsonMapper().readValue(results.toString(), type);
                offset += pageSize;
                if(parsedResults.size() == 0){
                    finished = true;
                }
                return parsedResults.iterator();
            } catch(HttpException e){
                finished = true;
                e.request(Optional.of(request));
                throw new RuntimeException(e);
            } catch(Exception e){
                finished = true;
                throw new RuntimeException(new HttpException.Builder()
                        .withMessage("Unexpected error fetching next page from remote")
                        .causedBy(e)
                        .requestContext(request)
                        .build());
            }
        }
    }
}
