package io.pulumi.deployment;

import com.google.common.collect.ImmutableMap;
import io.pulumi.core.Tuples;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MyMocks implements Mocks {

    @Override
    public CompletableFuture<Tuples.Tuple2<Optional<String>, Object>> newResourceAsync(MockResourceArgs args) {
        Objects.requireNonNull(args.type);
        switch (args.type) {
            case "aws:ec2/instance:Instance":
                return CompletableFuture.completedFuture(
                        Tuples.of(Optional.of("i-1234567890abcdef0"), ImmutableMap.of("publicIp", "203.0.113.12"))
                );
            case "pkg:index:MyCustom":
                return CompletableFuture.completedFuture(
                        Tuples.of(Optional.of(args.name + "_id"), args.inputs)
                );
            default:
                throw new IllegalArgumentException(String.format("Unknown resource '%s'", args.type));
        }
    }

    @Override
    public CompletableFuture<Map<String, Object>> callAsync(MockCallArgs args) {
        return CompletableFuture.completedFuture(null);
    }
}
