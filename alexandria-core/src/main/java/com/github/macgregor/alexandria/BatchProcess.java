package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.exceptions.BatchProcessException;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * Generic class to handle wrapping batch processing in robust error handling.
 *
 * This is generally used for processing collections of {@link com.github.macgregor.alexandria.Config.DocumentMetadata}
 * where we want to ensure we process all documents in the collection without losing any exceptions and their execution
 * context. The exception handling is boiler plate that was being duplicated all over, this abstracts it.
 *
 * A batch process has three parts:
 * <ol>
 *  <li><b>collect</b> - call the delegated {@link Batch#collect(Context)} method to collect the objects to process</li>
 *  <li><b>execute</b> - for each object collected, call the delegated {@link Task#execute(Context, Object)}</li>
 *  <li><b>after batch</b> - call the delegated {@link AfterBatch#execute(Context, Collection)}</li>
 * </ol>
 *
 * For example, {@link AlexandriaConvert} is implemented roughly as:
 *
 * <pre>
 * {@code
 * BatchProcess<Config.DocumentMetadata> batchProcess = new BatchProcess<>(context);
 * batchProcess.execute(
 *      context -> { // lambda implemented {@link Batch}
 *          context.config().metadata().get()
 *      },
 *      (context, metadata) -> { // lambda implemented {@link Task}
 *          AlexandriaConvert.convert(context, metadata);
 *      },
 *      (context, exceptions) -> { // lambda implemented {@link AfterBatch}
 *          Alexandria.save(context);
 *          return BatchProcess.EXCEPTIONS_UNHANDLED; //delegate exception handling to {@link BatchProcess}
 *      });
 * }
 * </pre>
 *
 * TODO:
 * Errors thrown during batch processing are currently a bit hard to read. It can be hard to tell exactly where an exception
 * was thrown. This needs to be improved.
 *
 * @param <T> type of the object being processed
 */
@Slf4j
@ToString
@Getter @Setter @Accessors(fluent = true)
@NoArgsConstructor @RequiredArgsConstructor @AllArgsConstructor
public class BatchProcess<T> {

    public static final Boolean EXCEPTIONS_HANDLED = true;
    public static final Boolean EXCEPTIONS_UNHANDLED = false;

    @NonNull private Context context;
    private Collection<AlexandriaException> exceptions = new ArrayList<>();

    /**
     * Execute the batch, providing a default {@link AfterBatch} that calls {@link Alexandria#save(Context)} before
     * throwing any errors that occurred.
     *
     * @see Batch#execute(Batch, Task, AfterBatch)
     *
     * @param batch  Batch collection delegate
     * @param task  Task execution delegate
     * @throws BatchProcessException  Wrapper containing all exceptions thrown while processing the batch
     */
    public void execute(Batch<T> batch, Task<T> task) throws BatchProcessException {
        execute(batch, task, (context, exceptions) -> {
            Alexandria.save(context);
            return EXCEPTIONS_UNHANDLED;
        });
    }

    /**
     * Execute the batch wrapping the delegated methods in robust exception handling.
     *
     * This method should guarantee all exceptions thrown, even runtime exceptions are wrapped and thrown as a checked
     * {@link BatchProcessException} at the end of processing. This allows as much of the batch to be processed as possible
     * before throwing an error. If a single object has a problem, why fail the whole thing?
     *
     * @param batch  Batch collection delegate
     * @param task  Task execution delegate
     * @param after  After batch delegate
     * @throws BatchProcessException  Wrapper containing all exceptions thrown while processing the batch
     */
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

    /**
     * Convenience method to remove some boiler plate from wrapping exceptions.
     *
     * @param cause  Cause being wrapped
     * @param t  Type of object being processed, if processing {@link com.github.macgregor.alexandria.Config.DocumentMetadata} we add it for debugging context
     * @param message  optional error message
     * @return  new exception ready to be thrown
     */
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

    /**
     * Lambda compatible interface that will collect objects to be processed
     *
     * @param <T>  type of the object being processed
     */
    @FunctionalInterface
    public interface Batch<T> {
        /**
         * Delegated method to collect objects for processing.
         * @param context  Alexandria context that may or may not be needed by caller.
         * @return  collection of objects to process
         * @throws Exception  Critical error collecting documents
         */
        Collection<T> collect(Context context) throws Exception;
    }

    /**
     * Lambda compatible interface that will process each object returned by {@link Batch#collect(Context)}.
     *
     * @param <T>  type of the object being processed
     */
    @FunctionalInterface
    public interface Task<T> {
        /**
         * Delegated method to process an individual object.
         *
         * @param context  Alexandria context that may or may not be needed by caller.
         * @param t  Object to process
         * @throws Exception  Error processing object, the rest of the batch will continue processing
         */
        void execute(Context context, T t) throws Exception;
    }

    /**
     * Lambda compatible interface that will be called after processing is complete
     *
     * @param <T>  type of the object being processed
     */
    @FunctionalInterface
    public interface AfterBatch<T> {
        /**
         * Delegated method after a batch is completed, useful for logging results or saving state.
         *
         * @param context  Alexandria context that may or may not be needed by caller.
         * @param exceptions  Any exceptions thrown during processing, or empty list if no errors occurred
         * @return  False if exceptions are not being handled by the caller (triggering {@link Batch#execute(Batch, Task, AfterBatch)}
         *          to thrown a {@link BatchProcessException}, or true if the errors have been handled and no {@link BatchProcessException}
         *          will be thrown.
         * @throws Exception  This exception will be wrapped by {@link Batch#execute(Batch, Task, AfterBatch)} just like
         *                    exceptions at any other phase and thrown in a {@link BatchProcessException}
         */
        boolean execute(Context context, Collection<AlexandriaException> exceptions) throws Exception;
    }
}
