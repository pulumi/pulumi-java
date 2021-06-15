package io.pulumi.resources;

import com.google.common.collect.ImmutableSet;
import io.pulumi.core.OutputDefault;

/**
 * A @see {@link Resource} that is used to indicate that an @see {@link io.pulumi.core.Output}
 * has a dependency on a particular resource. These resources are only created when dealing
 * with remote component resources.
 */
public class DependencyResource extends CustomResource {
    public DependencyResource(String urn) {
        super("", "", ResourceArgs.Empty, true);
        ImmutableSet<Resource> resources = ImmutableSet.of(this);
        this.setUrn(OutputDefault.of(resources, urn));
    }
}
