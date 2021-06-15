package io.pulumi.resources.internal;

import com.google.common.collect.ImmutableSet;
import io.pulumi.core.OutputDefault;
import io.pulumi.resources.ProviderResource;
import io.pulumi.resources.Resource;
import io.pulumi.resources.ResourceArgs;

/**
 * A @see {@link io.pulumi.resources.Resource} that is used by the provider SDK
 * as a stand-in for a provider that is only used for its reference.
 * Its only valid properties are its URN and ID.
 */
public final class DependencyProviderResource extends ProviderResource {
    public DependencyProviderResource(String reference) {
        super("", "", ResourceArgs.Empty, /* no options */ null, true);

        var lastSep = reference.lastIndexOf("::");
        if (lastSep == -1) {
            throw new IllegalArgumentException(
                    String.format("Expected '::' in provider reference '%s'", reference));
        }
        var urn = reference.substring(0, lastSep);
        var id = reference.substring(lastSep + 2);

        ImmutableSet<Resource> resources = ImmutableSet.of(this);
        this.setUrn(OutputDefault.of(resources, urn));
        this.setId(OutputDefault.of(resources, id));
    }
}