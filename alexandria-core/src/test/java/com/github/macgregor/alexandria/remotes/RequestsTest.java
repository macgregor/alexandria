package com.github.macgregor.alexandria.remotes;

import okhttp3.HttpUrl;
import okhttp3.Request;
import org.junit.Test;

public class RequestsTest {

    @Test
    public void testRequestsAddQueryParametersHandlesNullMap(){
        HttpUrl.Builder builder = new HttpUrl.Builder();
        Requests.addQueryParameters(builder, null);
    }

    @Test
    public void testRequestsAddSegmentsHandlesNullList(){
        HttpUrl.Builder builder = new HttpUrl.Builder();
        Requests.addSegments(builder, null);
    }

    @Test
    public void testRequestsAddHeadersHandlesNullMap(){
        Request.Builder builder = new Request.Builder();
        Requests.addHeaders(builder, null);
    }
}
