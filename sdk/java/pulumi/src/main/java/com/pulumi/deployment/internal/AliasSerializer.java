package com.pulumi.deployment.internal;

import java.util.List;
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

        // NOTE: any OutputData extra information like secret-ness or dependencies accumulated in the process of
        // serializing the aliases is discarded here by only considering the value of the future; it might be a small
        // refinement to take it into account.
        return Internal.of(o).getValueOrDefault(ImmutableList.of());
    }

    private static Output<pulumirpc.AliasOuterClass.Alias> serializeAlias(Alias alias) {
        // Case 1: alias was created as Alias.noParent().
        if (alias.hasNoParent()) {
            return Output.of(serializeNoParentAlias());
        }
        // Case 2: alias was created as Alias.withUrn(String urn).
        if (alias.getUrn().isPresent()) {
            return Output.of(serializeUrnAlias(alias.getUrn().get()));
        }
        // Case 3: alias is a more complex Spec.
        return serializeSpecAlias(alias);
    }

    private static pulumirpc.AliasOuterClass.Alias serializeNoParentAlias() {
        return pulumirpc.AliasOuterClass.Alias.newBuilder()
            .setSpec(pulumirpc.AliasOuterClass.Alias.Spec.newBuilder().setNoParent(true).build())
            .build();
    }

    private static pulumirpc.AliasOuterClass.Alias serializeUrnAlias(String urn) {
        return pulumirpc.AliasOuterClass.Alias.newBuilder()
            .setUrn(urn)
            .build();
    }

    private static Output<String> resolveParentUrn(Alias alias) {
        if (alias.getParent().isPresent()) {
            return alias.getParent().get().urn();
        }
        return alias.getParentUrn().orElse(Output.of(""));
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
                                 .setParentUrn(parentUrn)
                                 .build())
                        .build();
            });
    }
}
