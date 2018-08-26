package com.github.macgregor.alexandria.exceptions;

import com.github.macgregor.alexandria.Config;
import lombok.*;
import lombok.experimental.Accessors;
import okhttp3.Request;
import okhttp3.Response;

import java.util.Optional;

/**
 * Exception used when making Http requests in the {@link com.github.macgregor.alexandria.remotes.Remote}.
 * <p>
 * In addition to the {@link com.github.macgregor.alexandria.Config.DocumentMetadata} context you get from {@link AlexandriaException},
 * this also contains the {@link Request} and {@link Response} objects when the error was triggered.
 */
@ToString
@NoArgsConstructor @AllArgsConstructor
@Getter @Setter @Accessors(fluent = true)
public class HttpException extends AlexandriaException {
    private Optional<Config.DocumentMetadata> metadata = Optional.empty();
    private Optional<Request> request = Optional.empty();
    private Optional<Response> response = Optional.empty();

    public HttpException(String message) {
        super(message);
    }

    public HttpException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpException(Throwable cause) {
        super(cause);
    }

    /**
     * Builder class to help create {@link HttpException) in a more fluent way.
     */
    public static class Builder {
        private Optional<Config.DocumentMetadata> metadata = Optional.empty();
        private Optional<Request> request = Optional.empty();
        private Optional<Response> response = Optional.empty();
        private Optional<String> message = Optional.empty();
        private Optional<Throwable> cause = Optional.empty();

        /**
         * Message to include with the exception.
         *
         * @param message
         * @return
         */
        public Builder withMessage(String message){
            this.message = Optional.ofNullable(message);
            return this;
        }

        /**
         * Exception that triggered this.
         *
         * @param cause
         * @return
         */
        public Builder causedBy(Throwable cause){
            this.cause = Optional.ofNullable(cause);
            return this;
        }

        /**
         * The {@link Request} object at the time the error occurred.
         *
         * @param request
         * @return
         */
        public Builder requestContext(Request request){
            this.request = Optional.ofNullable(request);
            return this;
        }

        /**
         * The {@link Response} object at the time the error occurred.
         *
         * @param response
         * @return
         */
        public Builder responseContext(Response response){
            this.response = Optional.ofNullable(response);
            return this;
        }

        /**
         * The {@link com.github.macgregor.alexandria.Config.DocumentMetadata} being processed when the error occurred.
         *
         * @param metadata
         * @return
         */
        public Builder metadataContext(Config.DocumentMetadata metadata){
            this.metadata = Optional.ofNullable(metadata);
            return this;
        }

        /**
         * Create an {@link HttpException} from the build configuration.
         *
         * @return
         */
        public HttpException build(){
            HttpException exception = new HttpException();
            if(message.isPresent() && cause.isPresent()){
                exception = new HttpException(message.get(), cause.get());
            } else if(message.isPresent()){
                exception = new HttpException(message.get());
            } else if(cause.isPresent()){
                exception = new HttpException(cause.get());
            }
            exception.metadata(metadata);
            exception.request(request);
            exception.response(response);
            return exception;
        }
    }
}