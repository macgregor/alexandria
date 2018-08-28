package com.github.macgregor.alexandria.exceptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Jackson;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Optional;

/**
 * Exception used when making Http requests in the {@link com.github.macgregor.alexandria.remotes.Remote}.
 * <p>
 * In addition to the {@link com.github.macgregor.alexandria.Config.DocumentMetadata} context you get from {@link AlexandriaException},
 * this also contains the {@link Request} and {@link Response} objects when the error was triggered.
 */
@Slf4j
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
     * Builder class to help create {@link HttpException} in a more fluent way.
     */
    public static class Builder {
        private Optional<Config.DocumentMetadata> metadata = Optional.empty();
        private Optional<Request> request = Optional.empty();
        private Optional<Response> response = Optional.empty();
        private Optional<String> message = Optional.empty();
        private Optional<Throwable> cause = Optional.empty();

        /**
         * @param message  Message to include with the exception.
         * @return  builder
         */
        public Builder withMessage(String message){
            this.message = Optional.ofNullable(message);
            return this;
        }

        /**
         * @param cause  Exception that triggered this.
         * @return  builder
         */
        public Builder causedBy(Throwable cause){
            this.cause = Optional.ofNullable(cause);
            return this;
        }

        /**
         * @param request  The {@link Request} object at the time the error occurred.
         * @return  builder
         */
        public Builder requestContext(Request request){
            this.request = Optional.ofNullable(request);
            return this;
        }

        /**
         * @param response  The {@link Response} object at the time the error occurred.
         * @return  builder
         */
        public Builder responseContext(Response response){
            this.response = Optional.ofNullable(response);
            return this;
        }

        /**
         * @param metadata  The {@link com.github.macgregor.alexandria.Config.DocumentMetadata} being processed when the error occurred.
         * @return  builder
         */
        public Builder metadataContext(Config.DocumentMetadata metadata){
            this.metadata = Optional.ofNullable(metadata);
            return this;
        }

        /**
         * Create an {@link HttpException} from the build configuration.
         *
         * @return  new exception that can be thrown
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

    @Override
    public String toString() {
        String metadataString = null;
        if(metadata.isPresent()){
            try{
                metadataString = Jackson.jsonMapper().writer().withDefaultPrettyPrinter().writeValueAsString(metadata.get());
            } catch(JsonProcessingException e){
                metadataString = metadata.get().toString();
            }
        }
        return "HttpException{\n" +
                "   message=\"" + ExceptionUtils.getMessage(this) + "\",\n" +
                "   rootCauseMessage=\"" + ExceptionUtils.getRootCauseMessage(this) + "\",\n" +
                "   metadata=\n" + metadataString + "\n" +
                "   request=" + (request.isPresent() ? request.get().toString() : null) + "\n" +
                "   response=" + (response.isPresent() ? response.get().toString() : null) + "\n" +
                "}\n";
    }
}