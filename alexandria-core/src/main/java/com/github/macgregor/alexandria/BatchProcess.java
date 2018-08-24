package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.exceptions.BatchProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class BatchProcess<T> {
    private static Logger log = LoggerFactory.getLogger(BatchProcess.class);

    private Context context;
    private Collection<AlexandriaException> exceptions = new ArrayList<>();

    public BatchProcess(){}

    public BatchProcess(Context context){
        this.context = context;
    }

    public void execute(Batch<T> batch, Task<T> task) throws BatchProcessException {
        execute(batch, task, (context, exceptions) -> false);
    }

    public void execute(Batch<T> batch, Task<T> task, AfterBatch<T> after) throws BatchProcessException {
        try {
            for (T t : batch.collect(context)) {
                try {
                    task.execute(context, t);
                } catch(AlexandriaException e){
                    exceptions.add(e);
                } catch(Exception e){
                    exceptions.add(buildAlexandriaException(e, Optional.of(t), Optional.of("Unexpected exception thrown processing task.")));
                }
            }
        } catch(BatchProcessException e){
            exceptions.addAll(e.getExceptions());
        } catch(AlexandriaException e){
            exceptions.add(e);
        } catch(Exception e){
            exceptions.add(buildAlexandriaException(e, Optional.empty(), Optional.of("Unexpected exception thrown processing batch.")));
        }

        boolean exceptionsHandled = after.execute(context, exceptions);
        if(exceptions.size() > 0 && ! exceptionsHandled){
            throw new BatchProcessException.Builder()
                    .withMessage("Alexandria batch error.")
                    .causedBy(exceptions)
                    .build();
        }
    }

    protected AlexandriaException buildAlexandriaException(Throwable cause, Optional<T> t, Optional<String> message){
        AlexandriaException.Builder exceptionBuilder = new AlexandriaException.Builder()
                .causedBy(cause);
        if(message.isPresent()){
            exceptionBuilder.withMessage(message.get());
        }
        if(t.isPresent() && t.get() instanceof Config.DocumentMetadata){
            exceptionBuilder.metadataContext((Config.DocumentMetadata) t.get());
        }
        return exceptionBuilder.build();
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Collection<AlexandriaException> getExceptions() {
        return exceptions;
    }

    public void setExceptions(Collection<AlexandriaException> exceptions) {
        this.exceptions = exceptions;
    }

    @FunctionalInterface
    public interface Batch<T> {
        Collection<T> collect(Context context) throws Exception;
    }

    @FunctionalInterface
    public interface Task<T> {
        void execute(Context context, T t) throws Exception;
    }

    @FunctionalInterface
    public interface AfterBatch<T> {
        boolean execute(Context context, Collection<AlexandriaException> exceptions) throws BatchProcessException;
    }
}
