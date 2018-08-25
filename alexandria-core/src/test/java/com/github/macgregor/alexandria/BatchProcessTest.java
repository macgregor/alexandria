package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.exceptions.BatchProcessException;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BatchProcessTest {

    private Context context = new Context();

    @Test
    public void testBatchProcessHandlesTaskThrowingAlexandriaException(){
        BatchProcess<String> batchProcess = new BatchProcess<>(context);
        assertThatThrownBy(() ->
            batchProcess.execute(context -> Collections.singleton("foo"), (context, item) -> {
                throw new AlexandriaException();
            })).isInstanceOf(BatchProcessException.class);
    }

    @Test
    public void testBatchProcessHandlesBatchCollectionThrowingAlexandriaException(){
        BatchProcess<String> batchProcess = new BatchProcess<>(context);
        assertThatThrownBy(() ->
                batchProcess.execute(context -> {throw new AlexandriaException();}, (context, item) -> {}))
                .isInstanceOf(BatchProcessException.class);
    }

    @Test
    public void testBatchProcessHandlesBatchCollectionThrowingBatchProcessingException(){
        BatchProcess<String> batchProcess = new BatchProcess<>(context);
        assertThatThrownBy(() ->
                batchProcess.execute(context -> {throw new BatchProcessException(Collections.singleton(new AlexandriaException()));}, (context, item) -> {}))
                .isInstanceOf(BatchProcessException.class);
    }

    @Test
    public void testBatchProcessHandlesEmptyBatchCollectionThrowingBatchProcessingException(){
        BatchProcess<String> batchProcess = new BatchProcess<>(context);
        assertThatThrownBy(() ->
                batchProcess.execute(context -> {throw new BatchProcessException();}, (context, item) -> {}))
                .isInstanceOf(BatchProcessException.class);
    }

    @Test
    public void testBatchProcessDoesntThrowExceptionHandledByCaller(){
        BatchProcess<String> batchProcess = new BatchProcess<>(context);
        assertThatCode(() ->
                batchProcess.execute(context -> Collections.singleton("foo"), (context, item) -> {
                    throw new AlexandriaException();
                }, (context, exceptions) -> true)).doesNotThrowAnyException();
    }
}
