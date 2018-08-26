package com.github.macgregor.alexandria.exceptions;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * Exception used to collect all exceptions that happen during the processing of a collection of objects.
 * <p>
 * This exception will not have a cause, but a list of causes in {@link #exceptions}. This makes the stack trace for
 * these exceptions not very helpful, but not impossible to debug.
 */
@ToString
@NoArgsConstructor @AllArgsConstructor
@Getter @Setter @Accessors(fluent = true)
public class BatchProcessException extends AlexandriaException {
    private Collection<AlexandriaException> exceptions = new ArrayList<>();

    public BatchProcessException(String message) {
        super(message);
    }

    /**
     * Builder class to help create {@link BatchProcessException) in a more fluent way.
     */
    public static class Builder {
        private Collection<AlexandriaException> exceptions;
        private Optional<String> message = Optional.empty();

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
         * List of exceptions which triggered this.
         * @param exceptions
         * @return
         */
        public Builder causedBy(Collection<AlexandriaException> exceptions){
            this.exceptions = exceptions;
            return this;
        }

        /**
         * Create an {@link BatchProcessException} from the build configuration.
         *
         * @return
         */
        public BatchProcessException build(){
            BatchProcessException exception = new BatchProcessException();
            if(message.isPresent()){
                exception = new BatchProcessException(message.get());
            }
            exception.exceptions(exceptions);
            return exception;
        }
    }
}
