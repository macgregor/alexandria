package com.github.macgregor.alexandria.remotes;

import com.github.macgregor.alexandria.Jackson;
import com.github.macgregor.alexandria.exceptions.HttpException;
import okhttp3.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.StringReader;
import java.util.Objects;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RemoteDocumentTest {


    @Test
    public void testGetCreatesGetRequest() throws HttpException {
        RemoteDocument<TestDocument> test = spy(minimalBuilder().build());
        doReturn(expected()).when(test).parseResponse(any());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Request actualValue = invocation.getArgument(0);
                assertThat(actualValue.method()).isEqualTo("GET");
                return null;
            }
        }).when(test).doRequest(any());
        test.get();
    }

    @Test
    public void testGETParsesGenericTypeFromResponse() throws IOException {
        MockWebServer server = setup(new MockResponse().setBody(Jackson.jsonMapper().writeValueAsString(expected())));
        RemoteDocument<TestDocument> test = spy(minimalBuilder().baseUrl(server.url("foo").toString()).build());
        assertThat(test.get()).isEqualTo(expected());
    }

    @Test
    public void testGetAddsHeadersToRequest() throws HttpException {
        RemoteDocument<TestDocument> test = spy(minimalBuilder().header("foo", "bar").build());
        doReturn(expected()).when(test).parseResponse(any());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Request actualValue = invocation.getArgument(0);
                assertThat(actualValue.header("foo")).isEqualTo("bar");
                return null;
            }
        }).when(test).doRequest(any());
        test.get();
    }

    @Test
    public void testGetAddsRequestToWrappedException() throws HttpException {
        RemoteDocument<TestDocument> test = spy(minimalBuilder().build());
        doReturn(expected()).when(test).parseResponse(any());
        doReturn(null).when(test).client();
        Throwable thrown = catchThrowable(() -> test.get());
        assertThat(thrown).isInstanceOf(HttpException.class);
        assertThat(((HttpException)thrown).request()).isPresent();
    }

    @Test
    public void testGetWrapsUncheckedException() throws HttpException {
        RemoteDocument<TestDocument> test = spy(minimalBuilder().build());
        doReturn(expected()).when(test).parseResponse(any());
        doReturn(null).when(test).route();
        Throwable thrown = catchThrowable(() -> test.get());
        assertThat(thrown).isInstanceOf(HttpException.class);
    }

    @Test
    public void testPutCreatesPutRequest() throws HttpException {
        RemoteDocument<TestDocument> test = spy(minimalBuilder().build());
        doReturn(expected()).when(test).parseResponse(any());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Request actualValue = invocation.getArgument(0);
                assertThat(actualValue.method()).isEqualTo("PUT");
                return null;
            }
        }).when(test).doRequest(any());
        test.put(expected());
    }

    @Test
    public void testPutParsesGenericTypeFromResponse() throws IOException {
        MockWebServer server = setup(new MockResponse().setBody(Jackson.jsonMapper().writeValueAsString(expected())));
        RemoteDocument<TestDocument> test = spy(minimalBuilder().baseUrl(server.url("foo").toString()).build());
        assertThat(test.put(expected())).isEqualTo(expected());
    }

    @Test
    public void testPutAddsHeadersToRequest() throws HttpException {
        RemoteDocument<TestDocument> test = spy(minimalBuilder().header("foo", "bar").build());
        doReturn(expected()).when(test).parseResponse(any());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Request actualValue = invocation.getArgument(0);
                assertThat(actualValue.header("foo")).isEqualTo("bar");
                return null;
            }
        }).when(test).doRequest(any());
        test.put(expected());
    }

    @Test
    public void testPutAddsRequestToWrappedException() throws HttpException {
        RemoteDocument<TestDocument> test = spy(minimalBuilder().build());
        doReturn(expected()).when(test).parseResponse(any());
        doReturn(null).when(test).client();
        Throwable thrown = catchThrowable(() -> test.put(expected()));
        assertThat(thrown).isInstanceOf(HttpException.class);
        assertThat(((HttpException)thrown).request()).isPresent();
    }

    @Test
    public void testPutWrapsUncheckedException() throws HttpException {
        RemoteDocument<TestDocument> test = spy(minimalBuilder().build());
        doReturn(expected()).when(test).parseResponse(any());
        doReturn(null).when(test).route();
        Throwable thrown = catchThrowable(() -> test.put(expected()));
        assertThat(thrown).isInstanceOf(HttpException.class);
    }

    @Test
    public void testPostCreatesPostRequest() throws HttpException {
        RemoteDocument<TestDocument> test = spy(minimalBuilder().build());
        doReturn(expected()).when(test).parseResponse(any());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Request actualValue = invocation.getArgument(0);
                assertThat(actualValue.method()).isEqualTo("POST");
                return null;
            }
        }).when(test).doRequest(any());
        test.post(expected());
    }

    @Test
    public void testPostParsesGenericTypeFromResponse() throws IOException {
        MockWebServer server = setup(new MockResponse().setBody(Jackson.jsonMapper().writeValueAsString(expected())));
        RemoteDocument<TestDocument> test = spy(minimalBuilder().baseUrl(server.url("foo").toString()).build());
        assertThat(test.post(expected())).isEqualTo(expected());
    }

    @Test
    public void testPostAddsHeadersToRequest() throws HttpException {
        RemoteDocument<TestDocument> test = spy(minimalBuilder().header("foo", "bar").build());
        doReturn(expected()).when(test).parseResponse(any());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Request actualValue = invocation.getArgument(0);
                assertThat(actualValue.header("foo")).isEqualTo("bar");
                return null;
            }
        }).when(test).doRequest(any());
        test.post(expected());
    }

    @Test
    public void testPostAddsRequestToWrappedException() throws HttpException {
        RemoteDocument<TestDocument> test = spy(minimalBuilder().build());
        doReturn(expected()).when(test).parseResponse(any());
        doReturn(null).when(test).client();
        Throwable thrown = catchThrowable(() -> test.post(expected()));
        assertThat(thrown).isInstanceOf(HttpException.class);
        assertThat(((HttpException)thrown).request()).isPresent();
    }

    @Test
    public void testPostWrapsUncheckedException() throws HttpException {
        RemoteDocument<TestDocument> test = spy(minimalBuilder().build());
        doReturn(expected()).when(test).parseResponse(any());
        doReturn(null).when(test).route();
        Throwable thrown = catchThrowable(() -> test.post(expected()));
        assertThat(thrown).isInstanceOf(HttpException.class);
    }


    @Test
    public void testDeleteCreatesDeleteRequest() throws HttpException {
        RemoteDocument<TestDocument> test = spy(minimalBuilder().build());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Request actualValue = invocation.getArgument(0);
                assertThat(actualValue.method()).isEqualTo("DELETE");
                return null;
            }
        }).when(test).doRequest(any());
        test.delete();
    }

    @Test
    public void testDeleteAddsHeadersToRequest() throws HttpException {
        RemoteDocument<TestDocument> test = spy(minimalBuilder().header("foo", "bar").build());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Request actualValue = invocation.getArgument(0);
                assertThat(actualValue.header("foo")).isEqualTo("bar");
                return null;
            }
        }).when(test).doRequest(any());
        test.delete();
    }

    @Test
    public void testDeleteAddsRequestToWrappedException(){
        RemoteDocument<TestDocument> test = spy(minimalBuilder().build());
        doReturn(null).when(test).client();
        Throwable thrown = catchThrowable(() -> test.delete());
        assertThat(thrown).isInstanceOf(HttpException.class);
        assertThat(((HttpException)thrown).request()).isPresent();
    }

    @Test
    public void testDeleteWrapsUncheckedException(){
        RemoteDocument<TestDocument> test = spy(minimalBuilder().build());
        doReturn(null).when(test).route();
        Throwable thrown = catchThrowable(() -> test.delete());
        assertThat(thrown).isInstanceOf(HttpException.class);
    }

    @Test
    public void testRequestBodyMarshalsFromGenericType() throws IOException {
        RemoteDocument<TestDocument> test = minimalBuilder().build();
        RequestBody requestBody = test.requestBody(expected());
        Buffer buffer = new Buffer();
        requestBody.writeTo(buffer);
        assertThat(buffer.readUtf8()).contains("foo", "1");
    }

    @Test
    public void testParseResponseMarshalsToGenericType() throws HttpException {
        Response response = mock(Response.class);
        ResponseBody responseBody = mock(ResponseBody.class);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.charStream()).thenReturn(new StringReader("{\"name\": \"foo\",\n\"id\": 1\n}"));
        assertThat(minimalBuilder().build().parseResponse(response)).isEqualTo(expected());
    }

    @Test
    public void testParseResponseWrapsUncheckedExceptions() {
        Response response = mock(Response.class);
        when(response.body()).thenThrow(RuntimeException.class);
        assertThatThrownBy(() -> minimalBuilder().build().parseResponse(response))
                .isInstanceOf(HttpException.class);
    }

    @Test
    public void testParseResponseWrapsCheckedExceptions() {
        Response response = mock(Response.class);
        ResponseBody responseBody = mock(ResponseBody.class);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.charStream()).thenReturn(new StringReader("{\"name\" \"foo\""));
        assertThatThrownBy(() -> minimalBuilder().build().parseResponse(response))
                .isInstanceOf(HttpException.class);
    }

    @Test
    public void testRouteStartsWithBaseUrl(){
        RemoteDocument<TestDocument> test = minimalBuilder().build();
        assertThat(test.route()).isEqualTo(HttpUrl.parse("https://www.google.com"));
    }

    @Test
    public void testRouteAddsPathSegments(){
        RemoteDocument<TestDocument> test = minimalBuilder().pathSegment("foo").pathSegment("bar").build();
        assertThat(test.route()).isEqualTo(HttpUrl.parse("https://www.google.com")
                .newBuilder()
                .addPathSegment("foo")
                .addPathSegment("bar")
                .build());
    }

    @Test
    public void testRouteAddsQueryParameters(){
        RemoteDocument<TestDocument> test = minimalBuilder().queryParameter("foo", "bar").build();
        assertThat(test.route()).isEqualTo(HttpUrl.parse("https://www.google.com").newBuilder().addQueryParameter("foo", "bar").build());
    }

    @Test
    public void testRequestTimeoutDefaultsTo30(){
        RemoteDocument<TestDocument> test = minimalBuilder().build();
        assertThat(test.client().readTimeoutMillis()).isEqualTo(30000);
        assertThat(test.client().writeTimeoutMillis()).isEqualTo(30000);
        assertThat(test.client().connectTimeoutMillis()).isEqualTo(30000);
    }

    @Test
    public void testRequestTimeoutOverrides(){
        RemoteDocument<TestDocument> test = minimalBuilder().requestTimeout(45).build();
        assertThat(test.client().readTimeoutMillis()).isEqualTo(45000);
        assertThat(test.client().writeTimeoutMillis()).isEqualTo(45000);
        assertThat(test.client().connectTimeoutMillis()).isEqualTo(45000);
    }

    public static TestDocument expected(){
        TestDocument testDocument = new TestDocument();
        testDocument.id = 1;
        testDocument.name = "foo";
        return testDocument;
    }

    public static class TestDocument{
        public Integer id;
        public String name;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestDocument that = (TestDocument) o;
            return Objects.equals(id, that.id) &&
                    Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
    }

    protected RemoteDocument.RemoteDocumentBuilder minimalBuilder(){
        return RemoteDocument.<TestDocument>builder()
                .baseUrl("https://www.google.com")
                .entity(TestDocument.class);
    }

    protected MockWebServer setup(MockResponse mockResponse) throws IOException {
        MockWebServer server = new MockWebServer();
        server.enqueue(mockResponse);
        server.start();

        return server;
    }
}
