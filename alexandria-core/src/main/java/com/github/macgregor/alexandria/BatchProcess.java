package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.exceptions.BatchProcessException;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@Slf4j
@ToString
@Getter @Setter @Accessors(fluent = true)
@NoArgsConstructor @RequiredArgsConstructor @AllArgsConstructor
public class BatchProcess<T> {

    public static final Boolean EXCEPTIONS_HANDLED = true;
    public static final Boolean EXCEPTIONS_UNHANDLED = false;

    @NonNull private Context context;
    private Collection<AlexandriaException> exceptions = new ArrayList<>();

    public void execute(Batch<T> batch, Task<T> task) throws BatchProcessException {
        execute(batch, task, (context, exceptions) -> {
            Alexandria.save(context);
            return EXCEPTIONS_UNHANDLED;
        });
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
            if(e.exceptions().isEmpty()){
                AlexandriaException alexandriaException = new AlexandriaException(e.getMessage(), e.getCause());
                alexandriaException.setStackTrace(e.getStackTrace());
                alexandriaException.metadata(e.metadata());
                exceptions.add(alexandriaException);
            }
            exceptions.addAll(e.exceptions());
        } catch(AlexandriaException e){
            exceptions.add(e);
        } catch(Exception e){
            exceptions.add(buildAlexandriaException(e, Optional.empty(), Optional.of("Unexpected exception thrown processing batch.")));
        }


        boolean exceptionsHandled = EXCEPTIONS_UNHANDLED;
        try {
            exceptionsHandled = after.execute(context, exceptions);;
        } catch(BatchProcessException e){
            exceptions.addAll(e.exceptions());
        } catch(AlexandriaException e){
            exceptions.add(e);
        } catch(Exception e){
            exceptions.add(buildAlexandriaException(e, Optional.empty(), Optional.of("Unexpected exception thrown processing after batch.")));
        }
        if(exceptions.size() > 0 && exceptionsHandled == EXCEPTIONS_UNHANDLED){
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
        boolean execute(Context context, Collection<AlexandriaException> exceptions) throws Exception;
    }
}
