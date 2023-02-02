// Copyright 2016-2023, Pulumi Corporation.  All rights reserved.

package com.pulumi.deployment.internal;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import com.pulumi.core.Alias;
import com.pulumi.core.Output;
import com.pulumi.core.internal.Internal;

class AliasSerializer {
    public static CompletableFuture<ImmutableList<pulumirpc.AliasOuterClass.Alias>> serializeAliases(List<Output<Alias>> aliases) {
        var o = Output.all(aliases)
            .apply(as -> Output.all(ImmutableList.copyOf(as).stream()
                                    .map(AliasSerializer::serializeAlias)
                                    .collect(Collectors.toList())))
            .applyValue(ImmutableList::copyOf);

        // NOTE: following other Pulumi SDKs, any OutputData extras like dependencies accumulated in the process of
        // serializing the aliases are discarded here by only considering the value.
        return Internal.of(o).getValueOrDefault(ImmutableList.of());
    }

    // Serialize an Alias to the protobuf model. The result is an Output since some of the Alias fields are typed as
    // Output.
    //
    // Protobuf model of alias definitions expects the grammar from [alias.proto]. Empty strings are treated the same as
    // an absent value.
    //
    // [alias.proto]: https://github.com/pulumi/pulumi/blob/master/proto/pulumi/alias.proto
    private static Output<pulumirpc.AliasOuterClass.Alias> serializeAlias(Alias alias) {
        // Case 1: alias was created as Alias.withUrn(String urn):
        if (alias.getUrn().isPresent()) {
            return Output.of(serializeUrnAlias(alias.getUrn().get()));
        }
        // Case 2: alias was created with a builder:
        return serializeSpecAlias(alias);
    }

    private static Output<pulumirpc.AliasOuterClass.Alias> serializeSpecAlias(Alias alias) {
        final var nameOutput = alias.getName().orElse(Output.of(""));
        final var typeOutput = alias.getType().orElse(Output.of(""));
        final var stackOutput = alias.getStack().orElse(Output.of(""));
        final var projectOutput = alias.getProject().orElse(Output.of(""));
        final var parentUrnOutput = resolveParentUrn(alias);
        return Output.tuple(nameOutput, typeOutput, stackOutput, projectOutput, parentUrnOutput)
            .applyValue(t -> {
                    final var name = t.t1;
                    final var type = t.t2;
                    final var stack = t.t3;
                    final var project = t.t4;
                    final var parentUrn = t.t5;
                    return pulumirpc.AliasOuterClass.Alias.newBuilder()
                        .setSpec(pulumirpc.AliasOuterClass.Alias.Spec.newBuilder()
                                 .setName(name)
                                 .setType(type)
                                 .setStack(stack)
                                 .setProject(project)
                                 .setParentUrn(parentUrn.orElse(""))
                                 .setNoParent(parentUrn.isEmpty())
                                 .build())
                        .build();
            });
    }

    private static pulumirpc.AliasOuterClass.Alias serializeUrnAlias(String urn) {
        return pulumirpc.AliasOuterClass.Alias.newBuilder()
            .setUrn(urn)
            .build();
    }

    // Returns an empty optional for the noParent case, or an URN for the parentUrn case.
    private static Output<Optional<String>> resolveParentUrn(Alias alias) {
        if (alias.hasNoParent()) {
            return Output.of(Optional.empty());
        }
        if (alias.getParent().isPresent()) {
            return alias.getParent().get().urn().applyValue(Optional::of);
        }
        return alias.getParentUrn().orElse(Output.of("")).applyValue(Optional::of);
    }
}
