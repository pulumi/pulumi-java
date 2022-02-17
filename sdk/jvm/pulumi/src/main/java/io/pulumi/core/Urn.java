package io.pulumi.core;

import com.google.common.base.Strings;
import io.pulumi.core.internal.annotations.InternalUse;
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
            Input<String> name,
            Input<String> type,
            @Nullable Resource parent,
            @Nullable Input<String> parentUrn,
            @Nullable Input<String> project,
            @Nullable Input<String> stack
    ) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);

        if (parent != null && parentUrn != null) {
            throw new IllegalArgumentException("Only one of 'parent' and 'parentUrn' can be non-null.");
        }

        Output<String> parentPrefix;
        if (parent != null || parentUrn != null) {
            var parentUrnOutput = parent != null
                    ? parent.getUrn()
                    : parentUrn.toOutput();

            parentPrefix = parentUrnOutput.applyValue(
                    parentUrnString -> parentUrnString.substring(0, parentUrnString.lastIndexOf("::")) + "$");
        } else {
            var stackName = stack == null ? Input.of(Deployment.getInstance().getStackName()) : stack;
            var projectName = project == null ? Input.of(Deployment.getInstance().getProjectName()) : project;
            parentPrefix = OutputDefault.format("urn:pulumi:%s::%s::", stackName, projectName);
        }

        return OutputDefault.format("%s%s::%s", parentPrefix, type, name);
    }

    public static String create(@Nullable String parent, String type, String name) {
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
                Deployment.getInstance().getStackName(), Deployment.getInstance().getProjectName(), type, name
        );
    }

    /**
     * Computes the alias that should be applied to a child
     * based on an alias applied to it's parent. This may involve changing the name of the
     * resource in cases where the resource has a named derived from the name of the parent,
     * and the parent name changed.
     */
    @InternalUse
    public static Output<Alias> internalInheritedChildAlias(
            String childName, String parentName, Input<String> parentAlias, String childType
    ) {
        Objects.requireNonNull(childName);
        Objects.requireNonNull(parentName);
        Objects.requireNonNull(parentAlias);
        Objects.requireNonNull(childType);

        // If the child name has the parent name as a prefix, then we make the assumption that
        // it was constructed from the convention of using '{name}-details' as the name of the
        // child resource.  To ensure this is aliased correctly, we must then also replace the
        // parent aliases name in the prefix of the child resource name.
        //
        // For example:
        // * name: "newapp-function"
        // * options.parent.__name: "newapp"
        // * parentAlias: "urn:pulumi:stackname::projectname::awsx:ec2:Vpc::app"
        // * parentAliasName: "app"
        // * aliasName: "app-function"
        // * childAlias: "urn:pulumi:stackname::projectname::aws:s3/bucket:Bucket::app-function"
        var aliasName = Output.of(childName);
        if (childName.startsWith(parentName)) {
            aliasName = parentAlias.toOutput().applyValue(
                    (String parentAliasUrn) -> parentAliasUrn.substring(
                            parentAliasUrn.lastIndexOf("::") + 2) + childName.substring(parentName.length()));
        }

        var urn = create(
                aliasName.toInput(), Input.of(childType), null, parentAlias, null, null);

        return urn.applyValue(Alias::withUrn);
    }
}
