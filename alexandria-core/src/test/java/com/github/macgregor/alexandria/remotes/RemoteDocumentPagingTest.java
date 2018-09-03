package com.github.macgregor.alexandria.remotes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.macgregor.alexandria.Jackson;
import com.github.macgregor.alexandria.exceptions.HttpException;
import lombok.EqualsAndHashCode;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.LogManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RemoteDocumentPagingTest {

    static{
        LogManager.getLogManager().reset();
    }

    private MockWebServer server;
    private RemoteDocument.RemoteDocumentBuilder<TestDocument> remoteDocumentBuilder;

    @Before
    public void setup() throws IOException {
        server = new MockWebServer();
        server.start();
        remoteDocumentBuilder = minimalBuilder();
        remoteDocumentBuilder.baseUrl(server.url("foo").toString());
    }

    @Test
    public void testRemoteDocumentPagingSinglePage() throws IOException {
        List<TestDocument> expected = expected(10);
        RemoteDocument<TestDocument> test = remoteDocumentBuilder.build();
        mockResponses(Arrays.asList(
                new MockResponse().setBody(expectedPageBody(expected, 0, 10)),
                new MockResponse().setBody(expectedPageBody(expected, 10, 10))
        ));

        int i = 0;
        for(TestDocument document : test.getPaged()){
            System.out.println(document.name);
            assertThat(document.name).isEqualTo(String.format("foo-%d", i));
            assertThat(document.id).isEqualTo(i);
            i++;
        }
    }

    @Test
    public void testRemoteDocumentPagingMultiplePage() throws IOException {
        List<TestDocument> expected = expected(50);
        mockResponses(Arrays.asList(
                new MockResponse().setBody(expectedPageBody(expected, 0, 25)),
                new MockResponse().setBody(expectedPageBody(expected, 25, 25)),
                new MockResponse().setBody(expectedPageBody(expected, 50, 25))
        ));
        RemoteDocument<TestDocument> test = remoteDocumentBuilder.build();

        int i = 0;
        for(TestDocument document : test.getPaged()){
            System.out.println(document.name);
            assertThat(document.name).isEqualTo(String.format("foo-%d", i));
            assertThat(document.id).isEqualTo(i);
            i++;
        }
    }

    @Test
    public void testRemoteDocumentWrapsExceptionsInRuntimeException() throws IOException {
        mockResponses(Arrays.asList(
                new MockResponse().setResponseCode(404)
        ));
        RemoteDocument<TestDocument> test = remoteDocumentBuilder.build();

        assertThatThrownBy(() -> test.getPaged().iterator().hasNext())
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(HttpException.class);
    }

    @Test
    public void testRemoteDocumentPagingFirstResult() throws IOException {
        List<TestDocument> expected = expected(10);
        RemoteDocument<TestDocument> test = remoteDocumentBuilder.build();
        mockResponses(Arrays.asList(
                new MockResponse().setBody(expectedPageBody(expected, 0, 10)),
                new MockResponse().setBody(expectedPageBody(expected, 10, 10))
        ));

        TestDocument result = test.getPaged().first();
        assertThat(result.id).isEqualTo(0);
        assertThat(result.name).isEqualTo("foo-0");
    }

    @Test
    public void testRemoteDocumentPagingFirstResultNull() throws IOException {
        List<TestDocument> expected = expected(10);
        RemoteDocument<TestDocument> test = remoteDocumentBuilder.build();
        mockResponses(Arrays.asList(
                new MockResponse().setBody(expectedPageBody(expected, 10, 10))
        ));

        TestDocument result = test.getPaged().first();
        assertThat(result).isNull();
    }

    public static String expectedPageBody(List<TestDocument> expected, int offset, int count) throws JsonProcessingException {
        return String.format("{\"itemsPerPage\": %d,\n\"list\": %s,\n\"startIndex\": %d\n}", count, Jackson.jsonMapper().writeValueAsString(expectedPage(expected, offset, count)), offset);
    }

    public static List<TestDocument> expectedPage(List<TestDocument> expected, int offset, int count){
        int upper = (offset + count > expected.size()) ? expected.size() : offset + count;
        try {
            return expected.subList(offset, upper);
        } catch(IndexOutOfBoundsException | IllegalArgumentException e){
            return new ArrayList<>();
        }
    }

    public static List<TestDocument> expected(int size){
        List<TestDocument> expected = new ArrayList();

        for(int i = 0; i < size; i++){
            TestDocument testDocument = new TestDocument();
            testDocument.id = i;
            testDocument.name = String.format("foo-%d", i);
            expected.add(testDocument);
        }
        return expected;
    }

    @EqualsAndHashCode
    public static class TestDocument{
        public Integer id;
        public String name;
    }

    protected RemoteDocument.RemoteDocumentBuilder minimalBuilder(){
        return RemoteDocument.<TestDocument>builder()
                .baseUrl("https://www.google.com")
                .requestTimeout(1)
                .entity(TestDocument.class);
    }

    protected void mockResponses(List<MockResponse> mockResponses){
        for(MockResponse response : mockResponses) {
            server.enqueue(response);
        }
    }
}
