// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.machinelearningservices;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.machinelearningservices.inputs.GetMachineLearningDatasetArgs;
import io.pulumi.azurenative.machinelearningservices.outputs.GetMachineLearningDatasetResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetMachineLearningDataset {
    private GetMachineLearningDataset() {}
    /**
         * Machine Learning dataset object wrapped into ARM resource envelope.
     * API Version: 2020-05-01-preview.
     * 
     *
         * Machine Learning dataset object wrapped into ARM resource envelope.
     * 
     */
    public static CompletableFuture<GetMachineLearningDatasetResult> invokeAsync(GetMachineLearningDatasetArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:machinelearningservices:getMachineLearningDataset", TypeShape.of(GetMachineLearningDatasetResult.class), args == null ? GetMachineLearningDatasetArgs.Empty : args, Utilities.withVersion(options));
    }
}