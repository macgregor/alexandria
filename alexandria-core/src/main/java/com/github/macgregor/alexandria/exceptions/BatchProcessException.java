package com.github.macgregor.alexandria.exceptions;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@ToString
@NoArgsConstructor @AllArgsConstructor
@Getter @Setter @Accessors(fluent = true)
public class BatchProcessException extends AlexandriaException {
    private Collection<AlexandriaException> exceptions = new ArrayList<>();

    public BatchProcessException(String message) {
        super(message);
    }

    public static class Builder {
        private Collection<AlexandriaException> exceptions;
        private Optional<String> message = Optional.empty();

        public Builder withMessage(String message){
            this.message = Optional.ofNullable(message);
            return this;
        }

        public Builder causedBy(Collection<AlexandriaException> exceptions){
            this.exceptions = exceptions;
            return this;
        }

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
