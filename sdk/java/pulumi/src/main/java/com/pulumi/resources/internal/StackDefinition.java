package com.pulumi.resources.internal;

import com.pulumi.core.Output;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.resources.ResourceTransformation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Defines the Stack information used in the registration.
 */
@InternalUse
@ParametersAreNonnullByDefault
public class StackDefinition extends ComponentResource {

    /**
     * The type name that should be used to construct the root component in the tree of Pulumi resources
     * allocated by a deployment. This must be kept up to date with
     * "github.com/pulumi/pulumi/sdk/v3/go/common/resource/stack.RootStackType".
     */
    @InternalUse
    public static final String RootPulumiStackTypeName = "pulumi:pulumi:Stack";

    /**
     * Create and register a Stack. To finish registration use {@link #registerOutputs(Output)}
     *
     * @param projectName the current project name
     * @param stackName the current stack name
     * @param resourceTransformations optional list of transformations to apply to this stack's resources during construction.
     *      The transformations are applied in order, and are applied after all the transformations of custom
     *      and component resources in the stack.
     */
    @InternalUse
    public StackDefinition(
            String projectName,
            String stackName,
            List<ResourceTransformation> resourceTransformations
    ) {
        super(
                RootPulumiStackTypeName,
                rootPulumiStackName(projectName, stackName),
                ComponentResourceOptions.builder().resourceTransformations(
                        resourceTransformations
                ).build()
        );
    }

    private static String rootPulumiStackName(String projectName, String stackName) {
        return String.format("%s-%s", requireNonNull(projectName), requireNonNull(stackName));
    }

    /**
     * Finish the stack registration by registering its outputs.
     * @param outputs the stack outputs to register
     */
    @Override
    public void registerOutputs(Output<Map<String, Output<?>>> outputs) {
        super.registerOutputs(outputs);
    }
}
