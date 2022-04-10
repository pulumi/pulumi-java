// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.awsnative.wisdom;

import io.pulumi.awsnative.Utilities;
import io.pulumi.awsnative.wisdom.inputs.GetKnowledgeBaseArgs;
import io.pulumi.awsnative.wisdom.outputs.GetKnowledgeBaseResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetKnowledgeBase {
    private GetKnowledgeBase() {}
    /**
         * Definition of AWS::Wisdom::KnowledgeBase Resource Type
     * 
     */
    public static CompletableFuture<GetKnowledgeBaseResult> invokeAsync(GetKnowledgeBaseArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("aws-native:wisdom:getKnowledgeBase", TypeShape.of(GetKnowledgeBaseResult.class), args == null ? GetKnowledgeBaseArgs.Empty : args, Utilities.withVersion(options));
    }
}