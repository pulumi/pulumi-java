package com.pulumi.core;

import com.pulumi.resources.CustomTimeouts;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static com.pulumi.core.OutputTests.waitForValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class UrnTest {

    @SuppressWarnings({"unused"})
    private static Stream<Arguments> testParseAndRender() {
        return Stream.of(
                arguments("urn:pulumi:dev::iac-workshop::pulumi:pulumi:Stack::iac-workshop-dev"),
                arguments("urn:pulumi:dev::iac-workshop::apigateway:index:RestAPI::helloWorldApi"),
                arguments("urn:pulumi:dev::workshop::apigateway:index:RestAPI$aws:apigateway/restApi:RestApi::helloWorldApi"),
                arguments("urn:pulumi:dev::workshop::apigateway:index:RestAPI$aws:lambda/permission:Permission::helloWorldApi-fa520765"),
                arguments("urn:pulumi:stage::demo::eks:index:Cluster$pulumi:providers:kubernetes::eks-provider"),
                arguments("urn:pulumi:defStack::defProject::kubernetes:storage.k8s.io/v1beta1:CSIDriver::defName"),
                arguments("urn:pulumi:stack::project::my:my$aws:sns/topicSubscription:TopicSubscription::TopicSubscription"),
                arguments("urn:pulumi:foo::countdown::aws:cloudwatch/logSubscriptionFilter:LogSubscriptionFilter::countDown_watcher"),
                arguments("urn:pulumi:stack::project::pulumi:providers:aws::default_4_13_0"),
                arguments("urn:pulumi:foo::todo::aws:s3/bucketObject:BucketObject::todo4c238266/index.html"),
                arguments("urn:pulumi:dev::eks::pulumi:providers:aws::default_4_36_0")
        );
    }

    @ParameterizedTest
    @MethodSource
    void testParseAndRender(String example) {
        var urn = Urn.parse(example).asString();
        assertThat(urn).isEqualTo(example);
    }

    @SuppressWarnings({"unused"})
    private static Stream<Arguments> testCreateUrnOutput() {
        return Stream.of(
                arguments(
                        Output.of("stack"),
                        Output.of("project"),
                        Optional.empty(),
                        Output.of("package:type"),
                        Output.of("name"),
                        "urn:pulumi:stack::project::package:type::name"
                ),
                arguments(
                        Output.of("stack"),
                        Output.of("project"),
                        Optional.of(Output.of("urn:pulumi:stack::project::parentPackage:parentType::parentName")),
                        Output.of("package:type"),
                        Output.of("name"),
                        "urn:pulumi:stack::project::parentPackage:parentType$package:type::name"
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void testCreateUrnOutput(
            Output<String> stack,
            Output<String> project,
            Optional<Output<String>> parentUrn,
            Output<String> type,
            Output<String> name,
            String expected
    ) {
        var urn = Urn.create(stack, project, parentUrn, type, name);
        var value = waitForValue(urn);
        assertThat(value).isEqualTo(expected);
    }

    @SuppressWarnings({"unused"})
    private static Stream<Arguments> testCreateUrnString() {
        return Stream.of(
                arguments(
                        "stack", "project", Optional.empty(), "package:type", "name",
                        "urn:pulumi:stack::project::package:type::name"
                ),
                arguments(
                        "stack", "project",
                        Optional.of("urn:pulumi:stack::project::parentPackage:parentType::parentName"),
                        "package:type", "name",
                        "urn:pulumi:stack::project::parentPackage:parentType$package:type::name"
                ),
                arguments(
                        "stack", "project",
                        Optional.of("urn:pulumi:stack::project::parentPackage:parentType$package:type::name"),
                        "package2:type2", "name2",
                        "urn:pulumi:stack::project::package:type$package2:type2::name2"
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void testCreateUrnString(
            String stack, String project, Optional<String> parent, String type, String name, String expected
    ) {
        var urn = Urn.create(stack, project, parent, type, name);
        assertThat(urn).isEqualTo(expected);
    }

    @Test
    void testHashCodeEqualsContract() {
        EqualsVerifier.forClass(Urn.class).verify();
        EqualsVerifier.forClass(Urn.QualifiedTypeName.class).verify();
        EqualsVerifier.forClass(Urn.Type.class).verify();
    }
}
