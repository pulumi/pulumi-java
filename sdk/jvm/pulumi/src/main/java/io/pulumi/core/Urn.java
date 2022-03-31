package io.pulumi.core;

import com.google.common.base.Strings;
import io.pulumi.core.internal.OutputBuilder;
import io.pulumi.deployment.Deployment;
import io.pulumi.resources.Resource;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Objects;

/**
 * An automatically generated logical URN, used to stably identify resources. These are created
 * automatically by Pulumi to identify resources. They cannot be manually constructed.
 */
@ParametersAreNonnullByDefault
public class Urn {

    private Urn() {
        throw new UnsupportedOperationException("static class");
    }

    /**
     * Computes a URN from the combination of a resource name, resource type, optional parent,
     * optional project and optional stack.
     */
    public static Output<String> create(
            @Nullable Deployment deployment,
            Output<String> name,
            Output<String> type,
            @Nullable Resource parent,
            @Nullable Output<String> parentUrn,
            @Nullable Output<String> project,
            @Nullable Output<String> stack
    ) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);

        var out = OutputBuilder.forDeployment(deployment);

        if (parent != null && parentUrn != null) {
            throw new IllegalArgumentException("Only one of 'parent' and 'parentUrn' can be non-null.");
        }

        Output<String> parentPrefix;
        if (parent != null || parentUrn != null) {
            var parentUrnOutput = parent != null
                    ? parent.getUrn()
                    : parentUrn;

            parentPrefix = parentUrnOutput.applyValue(
                    parentUrnString -> parentUrnString.substring(0, parentUrnString.lastIndexOf("::")) + "$");
        } else {
            var stackName = stack == null ? out.of(deployment.getStackName()) : stack;
            var projectName = project == null ? out.of(deployment.getProjectName()) : project;
            parentPrefix = out.format("urn:pulumi:%s::%s::", stackName, projectName);
        }

        return out.format("%s%s::%s", parentPrefix, type, name);
    }

    public static String create(Deployment deployment, @Nullable String parent, String type, String name) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(name);

        if (!Strings.isNullOrEmpty(parent)) {
            var qualifiedType = parent.split("::")[2];
            var parentType = Arrays.stream(qualifiedType.split("\\$")).findFirst();
            if (parentType.isEmpty() || parentType.get().isBlank()) {
                throw new IllegalArgumentException(String.format("expected a parent with '$', got: '%s'", parent));
            }
            type = parentType.get() + "$" + type;
        }
        return "urn:pulumi:" + String.join("::",
                deployment.getStackName(), deployment.getProjectName(), type, name
        );
    }
}
