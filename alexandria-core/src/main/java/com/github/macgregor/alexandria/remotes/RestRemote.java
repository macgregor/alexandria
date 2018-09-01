package com.github.macgregor.alexandria.remotes;

import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.exceptions.HttpException;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;

import java.io.IOException;

/**
 * Optional base class remote implementations can extend to provide some basic functionality.
 */
@Slf4j
@Getter @Setter @Accessors(fluent = true)
public abstract class RestRemote {

    @NonNull protected OkHttpClient client;
    @NonNull protected Config.RemoteConfig config;

    /**
     * Execute a built {@link Request}, wrapping any errors in an {@link HttpException}.
     * TODO: update to take context and metadata for error context
     *
     * @param request  request to execute
     * @return  the response from the remote
     * @throws HttpException  if the response returned a non 20X status code, or general IO error making the call.
     */
    protected Response doRequest(Request request) throws HttpException {
        log.debug(request.toString());
        if(request.body() != null) {
            log.debug(bodyToString(request));
        }
        Call call = client.newCall(request);

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

        if (response.isSuccessful()) {
            return response;
        } else if (response.code() == 400) {
            throw httpException("Bad request.", request, response);
        } else if (response.code() == 403) {
            throw httpException("Unauthorized to access document.", request, response);
        } else if (response.code() == 404) {
            throw httpException("Document doesnt exist.", request, response);
        } else if (response.code() == 409) {
            throw httpException("Document conflicts with existing document.", request, response);
        } else {
            throw httpException(String.format("Unexpected status code %d.", response.code()), request, response);
        }
    }

    protected HttpException httpException(String message, Request request, Response response){
        return new HttpException.Builder()
                .withMessage(message)
                .requestContext(request)
                .responseContext(response)
                .build();
    }

    protected static String bodyToString(final Request request){
        try {
            final Request copy = request.newBuilder().build();
            final Buffer buffer = new Buffer();
            copy.body().writeTo(buffer);
            return buffer.readUtf8();
        } catch (final IOException e) {
            return "did not work";
        }
    }
}
