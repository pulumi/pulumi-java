package com.pulumi.example.unittest;

import com.pulumi.aws.ec2.Instance;
import com.pulumi.aws.ec2.SecurityGroup;
import com.pulumi.aws.ec2.outputs.SecurityGroupIngress;
import com.pulumi.core.Tuples;
import com.pulumi.deployment.MockCallArgs;
import com.pulumi.deployment.MockResourceArgs;
import com.pulumi.deployment.Mocks;
import com.pulumi.deployment.internal.DeploymentTests;
import com.pulumi.test.TestResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static com.pulumi.test.PulumiTest.extractValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatPredicate;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class WebserverInfraTest {

    private static DeploymentTests.DeploymentMock test;
    private static TestResult result;

    @BeforeAll
    public static void mockSetup() {
        test = DeploymentTests.DeploymentMockBuilder.builder()
                .setMocks(new MyMocks())
                .build();
        result = test.runTestAsync(WebserverInfra::stack).join();
    }

    @AfterAll
    static void cleanup() {
        cleanupDeploymentMocks();
    }

    @TestFactory
    Stream<DynamicTest> testInstanceHasNameTag() {
        var instance = result.resources().stream()
                .filter(r -> r instanceof Instance)
                .map(r -> (Instance) r)
                .findFirst();
        assertThat(instance).isPresent();

        return Stream.of(
                dynamicTest("instance has name tag", () -> {
                    var tags = instance.map(i -> i.tags())
                            .flatMap(ts -> extractValue(ts));
                    assertThat(tags)
                            .isPresent()
                            .hasValueSatisfying(v -> assertThat(v).containsKey("Name"));
                }),
                dynamicTest("instance must not use inline user data", () -> {
                    var userData = instance.map(i -> i.userData())
                            .map(ud -> extractValue(ud));
                    assertThat(userData).isEmpty();
                }),
                dynamicTest("security group must not have ssh ports open to internet", () -> {
                    result.resources().stream()
                            .filter(r -> r instanceof SecurityGroup)
                            .map(r -> (SecurityGroup) r)
                            .forEach(sg -> {
                                var ingress = extractValue(sg.ingress());
                                assertThatPredicate(
                                        (SecurityGroupIngress r) ->
                                                !(r.fromPort() == 22
                                                && r.cidrBlocks().stream()
                                                        .anyMatch(b -> b.equals("0.0.0.0/0")))
                                ).describedAs(
                                        "Illegal SSH port 22 open to the Internet (CIDR 0.0.0.0/0) on group %s",
                                        extractValue(sg.getUrn())
                                ).acceptsAll(ingress);
                            });
                })
        );
    }

    // Mock the engine state.
    public static class MyMocks implements Mocks {
        @Override
        public CompletableFuture<Tuples.Tuple2<Optional<String>, Object>> newResourceAsync(MockResourceArgs args) {
            return CompletableFuture.completedFuture(
                    Tuples.of(Optional.of(args.name + "_id"), args.inputs)
            );
        }

        @Override
        public CompletableFuture<Map<String, Object>> callAsync(MockCallArgs args) {
            return CompletableFuture.completedFuture(null); // not used
        }
    }
}