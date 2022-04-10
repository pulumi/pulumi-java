// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.awsnative.servicecatalog;

import io.pulumi.awsnative.Utilities;
import io.pulumi.awsnative.servicecatalog.inputs.GetCloudFormationProvisionedProductArgs;
import io.pulumi.awsnative.servicecatalog.outputs.GetCloudFormationProvisionedProductResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetCloudFormationProvisionedProduct {
    private GetCloudFormationProvisionedProduct() {}
    /**
         * Resource Schema for AWS::ServiceCatalog::CloudFormationProvisionedProduct
     * 
     */
    public static CompletableFuture<GetCloudFormationProvisionedProductResult> invokeAsync(GetCloudFormationProvisionedProductArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("aws-native:servicecatalog:getCloudFormationProvisionedProduct", TypeShape.of(GetCloudFormationProvisionedProductResult.class), args == null ? GetCloudFormationProvisionedProductArgs.Empty : args, Utilities.withVersion(options));
    }
}