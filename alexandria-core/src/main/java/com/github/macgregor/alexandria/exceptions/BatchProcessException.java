package com.github.macgregor.alexandria.exceptions;

import java.util.List;
import java.util.Optional;

public class BatchProcessException extends Exception {
    private List<AlexandriaException> exceptions;

    public BatchProcessException() {
    }

    public BatchProcessException(String message) {
        super(message);
    }

    public List<AlexandriaException> getExceptions() {
        return exceptions;
    }

    @Override
    public String toString() {


        return "BatchProcessException{" +
                "exceptions=" + exceptions +
                '}';
    }

    public void setExceptions(List<AlexandriaException> exceptions) {
        this.exceptions = exceptions;
    }

    public static class Builder {
        private List<AlexandriaException> exceptions;
        private Optional<String> message = Optional.empty();

        public Builder withMessage(String message){
            this.message = Optional.ofNullable(message);
            return this;
        }

        public Builder causedBy(List<AlexandriaException> exceptions){
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
