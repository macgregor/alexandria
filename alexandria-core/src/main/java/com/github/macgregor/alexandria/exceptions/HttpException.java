package com.github.macgregor.alexandria.exceptions;

import com.github.macgregor.alexandria.Config;
import okhttp3.Request;
import okhttp3.Response;

import java.util.Optional;

public class HttpException extends AlexandriaException {
    private Optional<Config.DocumentMetadata> metadata = Optional.empty();
    private Optional<Request> request = Optional.empty();
    private Optional<Response> response = Optional.empty();

    public HttpException() {}

    public HttpException(String message) {
        super(message);
    }

    public HttpException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpException(Throwable cause) {
        super(cause);
    }

    public Optional<Config.DocumentMetadata> getMetadata() {
        return metadata;
    }

    public void setMetadata(Optional<Config.DocumentMetadata> metadata) {
        this.metadata = metadata;
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

    public static class Builder {
        private Optional<Config.DocumentMetadata> metadata = Optional.empty();
        private Optional<Request> request = Optional.empty();
        private Optional<Response> response = Optional.empty();
        private Optional<String> message = Optional.empty();
        private Optional<Throwable> cause = Optional.empty();

        public Builder withMessage(String message){
            this.message = Optional.ofNullable(message);
            return this;
        }

        public Builder causedBy(Throwable cause){
            this.cause = Optional.ofNullable(cause);
            return this;
        }

        public Builder requestContext(Request request){
            this.request = Optional.ofNullable(request);
            return this;
        }

        public Builder responseContext(Response response){
            this.response = Optional.ofNullable(response);
            return this;
        }

        public Builder metadataContext(Config.DocumentMetadata metadata){
            this.metadata = Optional.ofNullable(metadata);
            return this;
        }

        public HttpException build(){
            HttpException exception = new HttpException();
            if(message.isPresent() && cause.isPresent()){
                exception = new HttpException(message.get(), cause.get());
            } else if(message.isPresent()){
                exception = new HttpException(message.get());
            } else if(cause.isPresent()){
                exception = new HttpException(cause.get());
            }
            exception.setMetadata(metadata);
            exception.setRequest(request);
            exception.setResponse(response);
            return exception;
        }
    }
}