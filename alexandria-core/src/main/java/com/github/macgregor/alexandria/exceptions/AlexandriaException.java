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

@Slf4j
@NoArgsConstructor @AllArgsConstructor
@Getter @Setter @Accessors(fluent = true)
public class AlexandriaException extends IOException {
    private Optional<Config.DocumentMetadata> metadata;

    public AlexandriaException(String message) {
        super(message);
    }

    public AlexandriaException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlexandriaException(Throwable cause) {
        super(cause);
    }

    public static class Builder {
        private Optional<Config.DocumentMetadata> metadata = Optional.empty();
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

        public Builder metadataContext(Config.DocumentMetadata metadata){
            this.metadata = Optional.ofNullable(metadata);
            return this;
        }

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
        log.debug(getMessage(), this);
        String metadataString = null;
        if(metadata.isPresent()){
            try{
                metadataString = Jackson.jsonWriter().writeValueAsString(metadata.get());
            } catch(JsonProcessingException e){
                metadataString = metadata.get().toString();
            }
        }
        return "AlexandriaException{\n" +
                "   message=\"" + ExceptionUtils.getMessage(this) + "\",\n" +
                "   rootCauseMessage=\"" + ExceptionUtils.getRootCauseMessage(this) + "\",\n" +
                "   metadata=" + metadataString + "\n" +
                "}\n";
    }
}
