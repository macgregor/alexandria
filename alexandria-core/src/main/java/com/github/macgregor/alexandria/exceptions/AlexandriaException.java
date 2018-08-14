package com.github.macgregor.alexandria.exceptions;

import com.github.macgregor.alexandria.Config;

import java.io.IOException;
import java.util.Optional;

public class AlexandriaException extends IOException {
    private Optional<Config.DocumentMetadata> metadata;

    public AlexandriaException() {
    }

    public AlexandriaException(String message) {
        super(message);
    }

    public AlexandriaException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlexandriaException(Throwable cause) {
        super(cause);
    }

    public Optional<Config.DocumentMetadata> getMetadata() {
        return metadata;
    }

    public void setMetadata(Optional<Config.DocumentMetadata> metadata) {
        this.metadata = metadata;
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
            exception.setMetadata(metadata);
            return exception;
        }
    }


}
