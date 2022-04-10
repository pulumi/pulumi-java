// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.migrate;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.migrate.inputs.GetSolutionArgs;
import io.pulumi.azurenative.migrate.outputs.GetSolutionResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetSolution {
    private GetSolution() {}
    /**
         * Solution REST Resource.
     * API Version: 2018-09-01-preview.
     * 
     *
         * Solution REST Resource.
     * 
     */
    public static CompletableFuture<GetSolutionResult> invokeAsync(GetSolutionArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:migrate:getSolution", TypeShape.of(GetSolutionResult.class), args == null ? GetSolutionArgs.Empty : args, Utilities.withVersion(options));
    }
}