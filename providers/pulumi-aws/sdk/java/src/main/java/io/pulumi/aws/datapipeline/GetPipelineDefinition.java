// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.aws.datapipeline;

import io.pulumi.aws.Utilities;
import io.pulumi.aws.datapipeline.inputs.GetPipelineDefinitionArgs;
import io.pulumi.aws.datapipeline.outputs.GetPipelineDefinitionResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetPipelineDefinition {
    private GetPipelineDefinition() {}
    /**
         * Provides details about a specific DataPipeline Pipeline Definition.
     * 
     * ## Example Usage
     * 
     *
         * A collection of arguments for invoking getPipelineDefinition.
     * 
     *
         * A collection of values returned by getPipelineDefinition.
     * 
     */
    public static CompletableFuture<GetPipelineDefinitionResult> invokeAsync(GetPipelineDefinitionArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("aws:datapipeline/getPipelineDefinition:getPipelineDefinition", TypeShape.of(GetPipelineDefinitionResult.class), args == null ? GetPipelineDefinitionArgs.Empty : args, Utilities.withVersion(options));
    }
}