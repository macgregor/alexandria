package com.github.macgregor.alexandria.exceptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.macgregor.alexandria.Config;
import com.github.macgregor.alexandria.Jackson;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.util.Optional;

/**
 * Exception thrown during processing to wrap the cause and provide runtime context about the error.
 * <p>
 * If the error occurred while processing a particular {@link com.github.macgregor.alexandria.Config.DocumentMetadata},
 * it will be included along with the cause and exception message.
 */
@Slf4j
@NoArgsConstructor @AllArgsConstructor
@Getter @Setter @Accessors(fluent = true)
@EqualsAndHashCode
public class AlexandriaException extends IOException {
    /** Contextual metadata when error ocurred. Default: none. */
    private Optional<Config.DocumentMetadata> metadata = Optional.empty();

    public AlexandriaException(String message) {
        super(message);
    }

    public AlexandriaException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlexandriaException(Throwable cause) {
        super(cause);
    }

    /**
     * Builder class to help create {@link AlexandriaException} in a more fluent way.
     */
    public static class Builder {
        private Optional<Config.DocumentMetadata> metadata = Optional.empty();
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
         * @param cause  Exception that triggered this
         * @return  builder
         */
        public Builder causedBy(Throwable cause){
            this.cause = Optional.ofNullable(cause);
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
         * Create an {@link AlexandriaException} from the build configuration.
         *
         * @return  new exception that can be thrown
         */
        public AlexandriaException build(){
            AlexandriaException exception = new AlexandriaException();
            if(message.isPresent() && cause.isPresent()){
                exception = new AlexandriaException(message.get(), cause.get());
            } else if(message.isPresent()){
                exception = new AlexandriaException(message.get());
            } else if(cause.isPresent()){
                exception = new AlexandriaException(cause.get());
            }
            exception.metadata(metadata);
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
        String rootCauseMessage = ExceptionUtils.getRootCauseMessage(this);
        return "AlexandriaException{\n" +
                "   message=\"" + ExceptionUtils.getMessage(this) + "\",\n" +
                "   rootCauseMessage=\"" + rootCauseMessage + "\",\n" +
                "   metadata=\n" + metadataString + "\n" +
                "}\n";
    }

    public void logStacktrace(){
        log.error(ExceptionUtils.getMessage(this), this);
    }
}
