package com.github.macgregor.alexandria.exceptions;

import java.util.Collection;
import java.util.Optional;

public class BatchProcessException extends AlexandriaException {
    private Collection<AlexandriaException> exceptions;

    public BatchProcessException() {
    }

    public BatchProcessException(String message) {
        super(message);
    }

    public Collection<AlexandriaException> getExceptions() {
        return exceptions;
    }

    @Override
    public String toString() {


        return "BatchProcessException{" +
                "exceptions=" + exceptions +
                '}';
    }

    public void setExceptions(Collection<AlexandriaException> exceptions) {
        this.exceptions = exceptions;
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
            exception.setExceptions(exceptions);
            return exception;
        }
    }
}
