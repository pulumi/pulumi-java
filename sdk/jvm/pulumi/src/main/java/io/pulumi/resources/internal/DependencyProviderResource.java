package io.pulumi.resources.internal;

import com.google.common.collect.ImmutableSet;
import io.pulumi.core.OutputDefault;
import io.pulumi.core.Tuples;
import io.pulumi.core.Tuples.Tuple2;
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
        super(parsePackage(reference), "", ResourceArgs.Empty, /* no options */ null, true);

        var urnAndId = parseReference(reference);

        ImmutableSet<Resource> resources = ImmutableSet.of(this);
        this.setUrn(OutputDefault.of(resources, urnAndId.t1));
        this.setId(OutputDefault.of(resources, urnAndId.t2));
    }

    private static String parsePackage(String reference) {
        var urn = parseReference(reference).t1;
        var urnParts = urn.split("::");
        var qualifiedType = urnParts[2];
        var qualifiedTypeParts = qualifiedType.split("\\$");
        var type = qualifiedTypeParts[qualifiedTypeParts.length - 1];
        var typeParts = type.split(":");
        // type will be "pulumi:providers:<package>" and we want the last part.
        return typeParts.length > 2 ? typeParts[2] : "";
    }

    private static Tuple2<String, String> parseReference(String reference) {
        var lastSep = reference.lastIndexOf("::");
        if (lastSep == -1) {
            throw new IllegalArgumentException(
                    String.format("Expected '::' in provider reference '%s'", reference));
        }
        var urn = reference.substring(0, lastSep);
        var id = reference.substring(lastSep + 2);
        return Tuples.of(urn, id);
    }
}