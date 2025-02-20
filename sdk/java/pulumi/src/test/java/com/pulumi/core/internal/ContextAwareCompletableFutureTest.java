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
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
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

    @Test
    void test() throws Exception {
        var checkIfValid = prepareContext();

        {
            var src = new CompletableFuture<Void>();
            var dst = ContextAwareCompletableFuture.wrap(src);

            var dst2 = dst.whenCompleteAsync((r, t) -> {
                checkIfValid.run();
            });

            src.complete(null);
            dst2.get();
        }

        var ex1 = assertThrows(ExecutionException.class, () -> {
            var src = new CompletableFuture<Void>();
            var dst = ContextAwareCompletableFuture.wrap(src);

            var dst2 = dst.whenComplete((r, t) -> {
                checkIfValid.run();
            });

            invalidateContext();
            src.complete(null);
            dst2.get();
        });
        Assertions.assertEquals(IllegalStateException.class, ex1.getCause().getClass());
    }
}