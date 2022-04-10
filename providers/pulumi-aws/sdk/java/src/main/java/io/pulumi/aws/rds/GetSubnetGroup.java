// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.aws.rds;

import io.pulumi.aws.Utilities;
import io.pulumi.aws.rds.inputs.GetSubnetGroupArgs;
import io.pulumi.aws.rds.outputs.GetSubnetGroupResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetSubnetGroup {
    private GetSubnetGroup() {}
    /**
         * Use this data source to get information about an RDS subnet group.
     * 
     * ## Example Usage
     * 
     *
         * A collection of arguments for invoking getSubnetGroup.
     * 
     *
         * A collection of values returned by getSubnetGroup.
     * 
     */
    public static CompletableFuture<GetSubnetGroupResult> invokeAsync(GetSubnetGroupArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("aws:rds/getSubnetGroup:getSubnetGroup", TypeShape.of(GetSubnetGroupResult.class), args == null ? GetSubnetGroupArgs.Empty : args, Utilities.withVersion(options));
    }
}