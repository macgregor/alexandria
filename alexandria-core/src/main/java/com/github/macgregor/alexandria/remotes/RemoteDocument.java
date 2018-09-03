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
 * The value of this class may be limited by variablility in rest api standards. If assumptions made by this class are
 * broken it may need to be updated and or made extensible or remotes to take advantage of it. The basics should apply, but
 * implementation details like POST requests returning the created document may not.
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
 * @param <T>  POJO representing the remote document for both requests and responses
 */
@Slf4j
@Getter @Setter @Accessors(fluent = true)
@Builder(toBuilder = true)
public class RemoteDocument<T>{
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /** Base url for requests */
    @NonNull private String baseUrl;

    /** Class matching T used to prevent runtime type erasure by JVM */
    @NonNull private Class<T> entity;

    /** Timeout for all requests. Default: 30 seconds. */
    @Builder.Default private Integer requestTimeout = 30;

    /** Headers to add to requests. */
    @Singular private Map<String, String> headers;

    /** Query parameters to add to requests */
    @Singular private Map<String, String> queryParameters;

    /** Path segments to add to {@link #baseUrl} to create request url. */
    @Singular private List<String> pathSegments;

    /** Currenly unimplemented. */
    @Singular private List<Integer> allowableStatusCodes;

    /** name of the query parameter used by the remote api to specify page offset */
    @Builder.Default private String pageOffsetRequestParameter = "startIndex";

    /** name of the query parameter used by the remote api to specify number of items per page */
    @Builder.Default private String pageSizeRequestParameter = "count";

    /** what field in the response contains the T entities to parse */
    @Builder.Default private String pageListResponseField = "list";

    /**
     * Get a single document from the remote, parsing the request result into a POJO.
     *
     * The builder should be configured such that the rest api will only return a single document, for example
     * www.jive.come/contents/1234 would return the Jive document with an id of 1234. It is on the remote implementation
     * to correctly use the api to achieve this.
     *
     * Exact semantics for how the remote interprets the request may vary.
     *
     * To retrieve multiple paged documents see {@link #getPaged()}.
     *
     * @return  A parsed POJO object representing the remote document
     * @throws HttpException  If a non 20X status code results from the request or an unchecked exception occurs.
     */
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

    /**
     * Get an iterable to page through remote documents matching the request.
     *
     * @see RemoteDocumentPage
     * @ee RemoteDocumentIterator
     *
     * @return  An iterable of all remote documents matching the request.
     */
    public RemoteDocumentPage<T> getPaged(){
        return new RemoteDocumentPage(this.toBuilder());
    }

    /**
     * Create a PUT request to update the remote document with the supplied values.
     *
     * The builder should be configured such that the rest api will only return a single document, for example
     * www.jive.come/contents/1234 would return the Jive document with an id of 1234. It is on the remote implementation
     * to correctly use the api to achieve this.
     *
     * Exact semantics for how the remote interprets the request may vary.
     *
     * @param t  The new values for the remote object
     * @return  The response from the server, expected to be the new updated remote document, but different rest apis may
     *          behave differently.
     * @throws HttpException  If a non 20X status code results from the request or an unchecked exception occurs.
     */
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

    /**
     * Create a POST request to create a remote document with the supplied values.
     *
     * The builder should be configured such that the rest api will only return a single document, for example
     * www.jive.come/contents/1234 would return the Jive document with an id of 1234. It is on the remote implementation
     * to correctly use the api to achieve this.
     *
     * Exact semantics for how the remote interprets the request may vary.
     *
     * @param t  The values to create the remote document with.
     * @return  The response from the server, expected to be the newly created remote document, but different rest apis may
     *          behave differently.
     * @throws HttpException  If a non 20X status code results from the request or an unchecked exception occurs.
     */
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

    /**
     * Create a DELETE request to delete a remote document.
     *
     * The builder should be configured such that the rest api will only return a single document, for example
     * www.jive.come/contents/1234 would return the Jive document with an id of 1234. It is on the remote implementation
     * to correctly use the api to achieve this.
     *
     * Exact semantics for how the remote interprets the request may vary.
     *
     * @throws HttpException  If a non 20X status code results from the request or an unchecked exception occurs.
     */
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

    /**
     * Convert the given POJO into a json object to use in a request body.
     *
     * @param t  The object to use in the request body
     * @return  {@link RequestBody} to be added to the request
     * @throws HttpException  Wrapper for any exception that occurs creating the request body
     */
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

    /**
     * Convert a response into a POJO
     *
     * @param response  The remote reponse to parse
     * @return  The parsed POJO
     * @throws HttpException  Wrapper for any exception that occurs creating the request body
     */
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

    /**
     * Turn the builder arguments into an {@link HttpUrl}
     *
     * @return  {@link HttpUrl}
     */
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

    /**
     * Perform the request with the remote api, adding some standard error checking.
     *
     * @param request  The request to perform.
     * @return  Response from the remote api
     * @throws HttpException  If a non 20X status code results from the request or an unchecked exception occurs.
     */
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

    /**
     * Iterable class used for pagination.
     *
     * @see RemoteDocumentIterator
     * @param <T>  POJO representing the remote document for both requests and responses
     */
    public static class RemoteDocumentPage<T> implements Iterable<T> {
        private RemoteDocumentBuilder requestBuilder;

        /**
         * Constructor that takes a {@link RemoteDocumentBuilder} to create paged requests.
         *
         * @param requestBuilder  Builder used by iterator to build paged requests
         */
        protected RemoteDocumentPage(RemoteDocumentBuilder requestBuilder){
            this.requestBuilder = requestBuilder;
        }

        /**
         * @see RemoteDocumentIterator
         * @return  Iterator that manages paging for caller
         */
        @Override
        public Iterator<T> iterator() {
            return new RemoteDocumentIterator(requestBuilder);
        }

        /**
         * Return the first result from the paged request.
         *
         * @return  The first object in the paged request or null if there are no results.
         * @throws HttpException  If a non 20X status code results from the request or an unchecked exception occurs.
         */
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

    /**
     * Class implementing {@link Iterator} to abstract paged requests for the caller.
     *
     * Allows you to iterate over a list of remote documents matching a request query in a simple for loop
     * <pre>
     * {@code
     * class Foo{
     *      Integer id;
     *      String name;
     *      String address;
     * }
     * RemoteDocument<Foo> remoteFoo = RemoteDocument.<Foo>builder()
     *      .baseUrl("www.google.com")
     *      .pathSegment("foo")
     *      .queryParameter("filter", "author(\"foo\")")
     *      .build()
     *
     * for(Foo foo : remoteFoo.getPaged()){
     *     //do stuff with foo
     * }
     * }
     * </pre>
     *
     * @param <T>
     */
    static class RemoteDocumentIterator<T> implements Iterator<T>{

        private RemoteDocumentBuilder requestBuilder;
        private Iterator<T> current;
        private Integer pageSize = 25;
        private Integer offset = 0;
        private boolean finished = false;

        /**
         * Constructor that takes a {@link RemoteDocumentBuilder} to create paged requests.
         *
         * @param requestBuilder  Builder used by iterator to build paged requests
         */
        protected RemoteDocumentIterator(RemoteDocumentBuilder requestBuilder){
            this.requestBuilder = requestBuilder;
        }

        /**
         * Determines if there are any more remote documents to iterate over.
         *
         * Responsible for making the actual request to the remote api to prime the current page, whether this is the first
         * request of if the current page has been exhausted. Will make n+1 requests to the remote where n is the number
         * of pages available.
         *
         * @see #nextPage()
         *
         * @return
         */
        @Override
        public boolean hasNext() {
            if(current == null || (!current.hasNext() && !finished)){
                current = nextPage();
            }
            return current.hasNext();
        }

        /**
         * Return the nexted parsed remote document from the local cache.
         *
         * @return  Parsed POJO representing the remote document.
         */
        @Override
        public T next() {
            return current.next();
        }

        /**
         * Make a request to the remote for a page of documents.
         *
         * Relies on several fields from the {@link RemoteDocumentBuilder}:
         * <ul>
         *     <li>{@link RemoteDocumentBuilder#pageSizeRequestParameter} - name of the query parameter used by the remote
         *     api to specify number of items per page</li>
         *     <li>{@link RemoteDocumentBuilder#pageOffsetRequestParameter} - name of the query parameter used by the remote
         *     api to specify page offset</li>
         *     <li>{@link RemoteDocumentBuilder#pageListResponseField} - what field in the response contains the T entities
         *     to parse</li>
         * </ul>
         *
         * @return  {@link Iterator} for the next page of data
         * @throws RuntimeException  Wrapper for any exceptions that occur making the request or processing results. The
         *                           {@link Iterator} interface wont let us throw checked exceptions.
         */
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
