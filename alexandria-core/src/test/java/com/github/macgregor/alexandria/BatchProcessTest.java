package com.github.macgregor.alexandria;

import com.github.macgregor.alexandria.exceptions.AlexandriaException;
import com.github.macgregor.alexandria.exceptions.BatchProcessException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

public class BatchProcessTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private Context context;

    @Before
    public void setup() throws IOException {
        context = TestData.minimalContext(folder);
    }

    @Test
    public void testBatchProcessHandlesTaskThrowingAlexandriaException(){
        BatchProcess<String> batchProcess = new BatchProcess<>(context);
        assertThatThrownBy(() ->
            batchProcess.execute(context -> Collections.singleton("foo"), (context, item) -> {
                throw new AlexandriaException();
            })).isInstanceOf(BatchProcessException.class);
    }

    @Test
    public void testBatchProcessHandlesTaskThrowingAlexandriaExceptionWithMetadata(){
        BatchProcess<String> batchProcess = new BatchProcess<>(context);
        assertThatThrownBy(() ->
                batchProcess.execute(context -> Collections.singleton("foo"), (context, item) -> {
                    throw new AlexandriaException.Builder().metadataContext(TestData.minimalDocumentMetadata(folder)).build();
                })).isInstanceOf(BatchProcessException.class);
    }

    @Test
    public void testBatchProcessHandlesTaskThrowingUncheckedException(){
        BatchProcess<String> batchProcess = new BatchProcess<>(context);
        assertThatThrownBy(() ->
                batchProcess.execute(context -> Collections.singleton("foo"), (context, item) -> {
                    throw new RuntimeException();
                })).isInstanceOf(BatchProcessException.class);
    }

    @Test
    public void testBatchProcessDocumentMetadataHandlesTaskThrowingExceptionWithEmptyExceptionDocument(){
        BatchProcess<Config.DocumentMetadata> batchProcess = new BatchProcess<>(context);
        Config.DocumentMetadata documentMetadata = new Config.DocumentMetadata();
        assertThatThrownBy(() ->
                batchProcess.execute(context -> Collections.singletonList(documentMetadata), (context, item) -> {
                    throw new AlexandriaException();
                })).isInstanceOf(BatchProcessException.class);
    }

    @Test
    public void testBatchProcessDocumentMetadataHandlesTaskThrowingExceptionWithExceptionDocument(){
        BatchProcess<Config.DocumentMetadata> batchProcess = new BatchProcess<>(context);
        Config.DocumentMetadata documentMetadata = new Config.DocumentMetadata();
        AlexandriaException expected = new AlexandriaException.Builder().metadataContext(documentMetadata).build();
        Throwable thrown = catchThrowable(() -> {
            batchProcess.execute(context -> Collections.singletonList(documentMetadata), (context, item) -> {
                throw expected;
            });
        });

        assertThat(thrown).isInstanceOf(BatchProcessException.class);
        BatchProcessException batchProcessException = (BatchProcessException)thrown;
        assertThat(batchProcessException.exceptions()).isNotEmpty();
        assertThat(batchProcessException.exceptions()).containsExactly(expected);
    }

    @Test
    public void testBatchProcessDocumentMetadataAddsDocumentMetadataToExceptionIfNotPresent(){
        BatchProcess<Config.DocumentMetadata> batchProcess = new BatchProcess<>(context);
        Config.DocumentMetadata documentMetadata = new Config.DocumentMetadata();
        AlexandriaException expected = new AlexandriaException.Builder().metadataContext(documentMetadata).build();
        Throwable thrown = catchThrowable(() -> {
            batchProcess.execute(context -> Collections.singletonList(documentMetadata), (context, item) -> {
                throw new AlexandriaException();
            });
        });

        assertThat(thrown).isInstanceOf(BatchProcessException.class);
        BatchProcessException batchProcessException = (BatchProcessException)thrown;
        assertThat(batchProcessException.exceptions()).containsExactly(expected);
    }

    @Test
    public void testBatchProcessHandlesBatchCollectionThrowingAlexandriaException(){
        BatchProcess<String> batchProcess = new BatchProcess<>(context);
        assertThatThrownBy(() ->
                batchProcess.execute(context -> {throw new AlexandriaException();}, (context, item) -> {}))
                .isInstanceOf(BatchProcessException.class);
    }

    @Test
    public void testBatchProcessHandlesBatchCollectionThrowingUncheckedException(){
        BatchProcess<String> batchProcess = new BatchProcess<>(context);
        assertThatThrownBy(() ->
                batchProcess.execute(context -> {throw new RuntimeException();}, (context, item) -> {}))
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

    @Test
    public void testBatchProcessExceptionBuilderNoMessage(){
        Throwable exception = new Exception();
        AlexandriaException actual = BatchProcess.buildAlexandriaException(exception, Optional.empty(), Optional.empty());
        assertThat(actual).hasCause(exception);
    }

    @Test
    public void testBatchProcessExceptionBuilderWithMessage(){
        Throwable exception = new Exception();
        AlexandriaException actual = BatchProcess.buildAlexandriaException(exception, Optional.empty(), Optional.of("hello"));
        assertThat(actual).hasMessage("hello");
    }

    @Test
    public void testBatchProcessExceptionBuilderWithDocumentMetadata(){
        Throwable exception = new Exception();
        Config.DocumentMetadata documentMetadata = new Config.DocumentMetadata();
        AlexandriaException actual = BatchProcess.buildAlexandriaException(exception, Optional.of(documentMetadata), Optional.empty());
        assertThat(actual.metadata()).isPresent();
        assertThat(actual.metadata().get()).isEqualTo(documentMetadata);
    }

    @Test
    public void testBatchProcessExceptionBuilderWithNonDocumentMetadataType(){
        Throwable exception = new Exception();
        Config.DocumentMetadata documentMetadata = new Config.DocumentMetadata();
        AlexandriaException actual = BatchProcess.buildAlexandriaException(exception, Optional.of(new Object()), Optional.empty());
        assertThat(actual.metadata()).isEmpty();
    }

    @Test
    public void testBatchProcessAfterCombinesThrownBatchProcessExceptions(){
        BatchProcess<Config.DocumentMetadata> batchProcess = new BatchProcess<>(context);
        Config.DocumentMetadata documentMetadata1 = new Config.DocumentMetadata();
        AlexandriaException documentMetadataException1 = new AlexandriaException.Builder()
                .metadataContext(documentMetadata1)
                .build();
        Config.DocumentMetadata documentMetadata2 = new Config.DocumentMetadata();
        AlexandriaException documentMetadataException2 = new AlexandriaException.Builder()
                .metadataContext(documentMetadata2)
                .build();
        AlexandriaException someOtherException = new AlexandriaException.Builder().build();

        Throwable thrown = catchThrowable(() -> {
            batchProcess.execute(
                context -> (Collection<Config.DocumentMetadata>) Arrays.asList(documentMetadata1, documentMetadata2),
                (context, item) -> {
                    throw new AlexandriaException();
                },
                (context, exceptions) -> {
                    throw new BatchProcessException.Builder()
                            .causedBy(Collections.singleton(someOtherException))
                            .build();
                });
        });

        BatchProcessException expected = new BatchProcessException.Builder()
                .withMessage("Alexandria batch error.")
                .causedBy(Arrays.asList(documentMetadataException1, documentMetadataException2, someOtherException))
                .build();
        assertThat(thrown).isInstanceOf(BatchProcessException.class);
        BatchProcessException batchProcessException = (BatchProcessException)thrown;
        assertThat(batchProcessException).isEqualTo(expected);
    }

    @Test
    public void testBatchProcessAfterAddsThrownAlexandriaExceptionsToBatchProcessException(){
        BatchProcess<Config.DocumentMetadata> batchProcess = new BatchProcess<>(context);
        Config.DocumentMetadata documentMetadata1 = new Config.DocumentMetadata();
        AlexandriaException documentMetadataException1 = new AlexandriaException.Builder()
                .metadataContext(documentMetadata1)
                .build();

        Throwable thrown = catchThrowable(() -> {
            batchProcess.execute(
                    context -> (Collection<Config.DocumentMetadata>) Arrays.asList(documentMetadata1),
                    (context, item) -> {
                        return;
                    },
                    (context, exceptions) -> {
                        throw documentMetadataException1;
                    });
        });

        BatchProcessException expected = new BatchProcessException.Builder()
                .withMessage("Alexandria batch error.")
                .causedBy(Arrays.asList(documentMetadataException1))
                .build();
        assertThat(thrown).isInstanceOf(BatchProcessException.class);
        BatchProcessException batchProcessException = (BatchProcessException)thrown;
        assertThat(batchProcessException).isEqualTo(expected);
    }
}
