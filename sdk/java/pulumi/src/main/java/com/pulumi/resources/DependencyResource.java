package com.pulumi.resources;

import com.google.common.collect.ImmutableSet;
import com.pulumi.core.Output;
import com.pulumi.core.internal.Internal;
import com.pulumi.core.internal.OutputInternal;

/**
 * A @see {@link Resource} that is used to indicate that an @see {@link com.pulumi.core.Output}
 * has a dependency on a particular resource. These resources are only created when dealing
 * with remote component resources.
 */
public class DependencyResource extends CustomResource {

    public DependencyResource(String urn) {
        super("", "", ResourceArgs.Empty, true);
        ImmutableSet<Resource> resources = ImmutableSet.of(this);
        Internal.from(this).setUrn(new OutputInternal<>(resources, urn));
        Internal.from(this).setId(Output.ofNullable(null));
    }
}
