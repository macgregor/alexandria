package com.github.macgregor.alexandria.remotes;

import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Optional;

public class HttpException extends IOException {
    private Optional<Request> request = Optional.empty();
    private Optional<Response> response = Optional.empty();

    public HttpException(){
        super();
    }
    public HttpException(String message){
        super(message);
    }
    public HttpException(Throwable t){
        super(t);
    }
    public HttpException(String message, Throwable t){
        super(message, t);
    }

    public Optional<Request> getRequest() {
        return request;
    }

    public void setRequest(Optional<Request> request) {
        this.request = request;
    }

    public Optional<Response> getResponse() {
        return response;
    }

    public void setResponse(Optional<Response> response) {
        this.response = response;
    }
}
