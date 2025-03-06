package com.pulumi.core.internal;

import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.deployment.CallOptions;
import com.pulumi.deployment.DeploymentInstance;
import com.pulumi.deployment.InvokeOptions;
import com.pulumi.deployment.InvokeOutputOptions;
import com.pulumi.deployment.internal.DeploymentImpl;
import com.pulumi.deployment.internal.DeploymentInstanceHolder;
import com.pulumi.resources.CallArgs;
import com.pulumi.resources.InvokeArgs;
import com.pulumi.resources.Resource;
import com.pulumi.resources.ResourceArgs;
import com.pulumi.resources.ResourceOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static org.junit.Assert.assertThrows;

class ContextAwareCompletableFutureTest {
    static class Context implements DeploymentInstance {
        private boolean isInvalid;

        @Override
        public DeploymentImpl.Config getConfig() {
            return null;
        }

        @Override
        public boolean isInvalid() {
            return isInvalid;
        }

        @Override
        public void markInvalid() {
            isInvalid = true;
        }

        @Nonnull
        @Override
        public String getStackName() {
            return "";
        }

        @Nonnull
        @Override
        public String getProjectName() {
            return "";
        }

        @Override
        public boolean isDryRun() {
            return false;
        }

        @Override
        public <T> Output<T> invoke(String token,
                                    TypeShape<T> targetType,
                                    InvokeArgs args) {
            return null;
        }

        @Override
        public <T> Output<T> invoke(String token,
                                    TypeShape<T> targetType,
                                    InvokeArgs args,
                                    @Nullable InvokeOptions options) {
            return null;
        }

        @Override
        public <T> Output<T> invoke(String token,
                                    TypeShape<T> targetType,
                                    InvokeArgs args,
                                    @Nullable InvokeOptions options,
                                    CompletableFuture<String> packageRef) {
            return null;
        }

        @Override
        public <T> Output<T> invoke(String token,
                                    TypeShape<T> targetType,
                                    InvokeArgs args,
                                    @Nullable InvokeOutputOptions options) {
            return null;
        }

        @Override
        public <T> Output<T> invoke(String token,
                                    TypeShape<T> targetType,
                                    InvokeArgs args,
                                    @Nullable InvokeOutputOptions options,
                                    CompletableFuture<String> packageRef) {
            return null;
        }

        @Override
        public CompletableFuture<Void> invokeAsync(String token,
                                                   InvokeArgs args) {
            return null;
        }

        @Override
        public CompletableFuture<Void> invokeAsync(String token,
                                                   InvokeArgs args,
                                                   InvokeOptions options) {
            return null;
        }

        @Override
        public <T> CompletableFuture<T> invokeAsync(String token,
                                                    TypeShape<T> targetType,
                                                    InvokeArgs args) {
            return null;
        }

        @Override
        public <T> CompletableFuture<T> invokeAsync(String token,
                                                    TypeShape<T> targetType,
                                                    InvokeArgs args,
                                                    InvokeOptions options) {
            return null;
        }

        @Override
        public <T> CompletableFuture<T> invokeAsync(String token,
                                                    TypeShape<T> targetType,
                                                    InvokeArgs args,
                                                    InvokeOptions options,
                                                    CompletableFuture<String> packageRef) {
            return null;
        }

        @Override
        public <T> Output<T> call(String token,
                                  TypeShape<T> targetType,
                                  CallArgs args,
                                  @Nullable Resource self,
                                  @Nullable CallOptions options) {
            return null;
        }

        @Override
        public <T> Output<T> call(String token,
                                  TypeShape<T> targetType,
                                  CallArgs args,
                                  @Nullable Resource self) {
            return null;
        }

        @Override
        public <T> Output<T> call(String token,
                                  TypeShape<T> targetType,
                                  CallArgs args) {
            return null;
        }

        @Override
        public void call(String token,
                         CallArgs args,
                         @Nullable Resource self,
                         @Nullable CallOptions options) {

        }

        @Override
        public void call(String token,
                         CallArgs args,
                         @Nullable Resource self) {

        }

        @Override
        public void call(String token,
                         CallArgs args) {

        }

        @Override
        public CompletableFuture<String> registerPackage(String baseProviderName,
                                                         String baseProviderVersion,
                                                         String baseProviderDownloadUrl,
                                                         String packageName,
                                                         String packageVersion,
                                                         String base64Parameter) {
            return null;
        }

        @Override
        public void readOrRegisterResource(Resource resource,
                                           boolean remote,
                                           Function<String, Resource> newDependency,
                                           ResourceArgs args,
                                           ResourceOptions options,
                                           Resource.LazyFields lazy,
                                           CompletableFuture<String> packageRef) {

        }

        @Override
        public void registerResourceOutputs(Resource resource,
                                            Output<Map<String, Output<?>>> outputs) {

        }
    }

    private Runnable prepareContext() {
        Context context = new Context();
        DeploymentInstanceHolder.setInstance(context);

        return new Runnable() {
            @Override
            public void run() {
                Assertions.assertEquals(context, DeploymentInstanceHolder.getInstance());
            }
        };
    }

    private void invalidateContext() {
        DeploymentInstanceHolder.getInstance().markInvalid();
    }

    private void executePositiveTest(Function<CompletableFuture<Void>, CompletableFuture<Void>> callback) throws Exception {
        var src = new CompletableFuture<Void>();
        var dst = ContextAwareCompletableFuture.wrap(src);

        dst.getNow(null);
        assertThrows(TimeoutException.class, () -> dst.get(1, TimeUnit.MICROSECONDS));

        var dst2 = callback.apply(dst);

        src.complete(null);
        dst2.get();
        dst2.get(1, TimeUnit.MICROSECONDS);
    }

    private void executeNegativeTest(Function<CompletableFuture<Void>, CompletableFuture<Void>> callback) {
        var ex1 = assertThrows(ExecutionException.class, () -> {
            var src = new CompletableFuture<Void>();
            var dst = ContextAwareCompletableFuture.wrap(src);

            var dst2 = callback.apply(dst);

            invalidateContext();
            src.complete(null);
            dst2.get();
        });
        Assertions.assertEquals(IllegalStateException.class, ex1.getCause().getClass());
    }

    private void executePositiveAndNegativeTest(Function<CompletableFuture<Void>, CompletableFuture<Void>> callback) throws Exception {
        executePositiveTest(callback);
        executeNegativeTest(callback);
    }

    @Test
    void acceptEither() throws Exception {
        var checkIfValid = prepareContext();

        executePositiveAndNegativeTest(f -> f.acceptEither(f, (v) -> {
            checkIfValid.run();
        }));
    }

    @Test
    void acceptEitherAsync() throws Exception {
        var checkIfValid = prepareContext();

        executePositiveAndNegativeTest(f -> f.acceptEitherAsync(f, (v) -> {
            checkIfValid.run();
        }));
    }

    @Test
    void applyToEither() throws Exception {
        var checkIfValid = prepareContext();

        executePositiveAndNegativeTest(f -> f.applyToEither(f, (v) -> {
            checkIfValid.run();
            return v;
        }));
    }

    @Test
    void applyToEitherAsync() throws Exception {
        var checkIfValid = prepareContext();

        executePositiveAndNegativeTest(f -> f.applyToEitherAsync(f, (v) -> {
            checkIfValid.run();
            return v;
        }));
    }

    @Test
    void completeAsync() throws Exception {
        var checkIfValid = prepareContext();

        var src = new CompletableFuture<Void>();
        var dst = ContextAwareCompletableFuture.wrap(src);

        var dst2 = dst.completeAsync(() -> {
            checkIfValid.run();
            return null;
        });

        dst2.get();
    }

    @Test
    void completeOnTimeout() throws Exception {
        var checkIfValid = prepareContext();

        var src = new CompletableFuture<Void>();
        var dst = ContextAwareCompletableFuture.wrap(src);

        var dst2 = dst.completeOnTimeout(null, 1, TimeUnit.MICROSECONDS);
        checkIfValid.run();
        dst2.get();
    }

    @Test
    void exceptionally() throws Exception {
        var checkIfValid = prepareContext();

        var src = new CompletableFuture<Void>();
        var dst = ContextAwareCompletableFuture.wrap(src);

        dst.getNow(null);
        assertThrows(TimeoutException.class, () -> dst.get(1, TimeUnit.MICROSECONDS));

        var dst2 = dst.exceptionally((t) -> {
            checkIfValid.run();
            return null;
        });

        dst.completeExceptionally(new RuntimeException());

        dst2.get();
    }

    @Test
    void handleAsync() throws Exception {
        var checkIfValid = prepareContext();

        executePositiveAndNegativeTest(f -> f.handleAsync((v, e) -> {
            checkIfValid.run();
            return v;
        }));
    }

    @Test
    void runAfterBoth() throws Exception {
        var checkIfValid = prepareContext();

        executePositiveAndNegativeTest(f -> f.runAfterBoth(f, () -> {
            checkIfValid.run();
        }));
    }

    @Test
    void runAfterBothAsync() throws Exception {
        var checkIfValid = prepareContext();

        executePositiveAndNegativeTest(f -> f.runAfterBothAsync(f, () -> {
            checkIfValid.run();
        }));
    }

    @Test
    void runAfterEither() throws Exception {
        var checkIfValid = prepareContext();

        executePositiveAndNegativeTest(f -> f.runAfterEither(f, () -> {
            checkIfValid.run();
        }));
    }

    @Test
    void runAfterEitherAsync() throws Exception {
        var checkIfValid = prepareContext();

        executePositiveAndNegativeTest(f -> f.runAfterEitherAsync(f, () -> {
            checkIfValid.run();
        }));
    }

    @Test
    void thenAcceptAsync() throws Exception {
        var checkIfValid = prepareContext();

        executePositiveAndNegativeTest(f -> f.thenAcceptAsync((f1) -> {
            checkIfValid.run();
        }));
    }

    @Test
    void thenAcceptBoth() throws Exception {
        var checkIfValid = prepareContext();

        executePositiveAndNegativeTest(f -> f.thenAcceptBoth(f, (f1, f2) -> {
            checkIfValid.run();
        }));
    }

    @Test
    void thenAcceptBothAsync() throws Exception {
        var checkIfValid = prepareContext();

        executePositiveAndNegativeTest(f -> f.thenAcceptBothAsync(f, (f1, f2) -> {
            checkIfValid.run();
        }));
    }

    @Test
    void thenCombine() throws Exception {
        var checkIfValid = prepareContext();

        executePositiveAndNegativeTest(f -> f.thenCombine(f, (f1, f2) -> {
            checkIfValid.run();
            return f2;
        }));
    }

    @Test
    void thenCombineAsync() throws Exception {
        var checkIfValid = prepareContext();

        executePositiveAndNegativeTest(f -> f.thenCombineAsync(f, (f1, f2) -> {
            checkIfValid.run();
            return f2;
        }));
    }

    @Test
    void thenComposeAsync() throws Exception {
        var checkIfValid = prepareContext();

        executePositiveAndNegativeTest(f -> f.thenComposeAsync((f1) -> {
            checkIfValid.run();
            return CompletableFuture.completedFuture(f1);
        }));
    }

    @Test
    void thenRun() throws Exception {
        var checkIfValid = prepareContext();

        executePositiveAndNegativeTest(f -> f.thenRun(() -> {
            checkIfValid.run();
        }));
    }

    @Test
    void thenRunAsync() throws Exception {
        var checkIfValid = prepareContext();

        executePositiveAndNegativeTest(f -> f.thenRunAsync(() -> {
            checkIfValid.run();
        }));
    }

    @Test
    void whenCompleteAsync() throws Exception {
        var checkIfValid = prepareContext();

        executePositiveAndNegativeTest(f -> f.whenCompleteAsync((r, t) -> {
            checkIfValid.run();
        }));
    }

}