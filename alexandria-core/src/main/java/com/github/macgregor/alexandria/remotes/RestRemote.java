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
            throw new HttpException.Builder()
                    .withMessage("Bad request.")
                    .requestContext(request)
                    .responseContext(response)
                    .build();
        } else if (response.code() == 403) {
            throw new HttpException.Builder()
                    .withMessage("Unauthorized to access document.")
                    .requestContext(request)
                    .responseContext(response)
                    .build();
        } else if (response.code() == 404) {
            throw new HttpException.Builder()
                    .withMessage("Document doesnt exist.")
                    .requestContext(request)
                    .responseContext(response)
                    .build();
        } else if (response.code() == 409) {
            throw new HttpException.Builder()
                    .withMessage("Document conflicts with existing document.")
                    .requestContext(request)
                    .responseContext(response)
                    .build();
        } else {
            throw new HttpException.Builder()
                    .withMessage(String.format("Unexpected status code %d.", response.code()))
                    .requestContext(request)
                    .responseContext(response)
                    .build();
        }
    }
}
