package com.pulumi.bootstrap.internal;

import com.google.gson.Gson;
import com.pulumi.bootstrap.internal.PulumiPlugins.RawResource;
import com.pulumi.core.internal.Optionals;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junitpioneer.jupiter.cartesian.ArgumentSets;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.MethodFactory;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class PulumiPluginTest {

    @CartesianTest
    @MethodFactory("testPluginResolverBruteForceSource")
    void testPluginResolverBruteForce(@Nullable RawResource rawPlugin, @Nullable RawResource rawVersion) {
        assumeThat(rawPlugin == null && rawVersion == null).isFalse();

        var plugin = PulumiPlugin.resolve(rawPlugin, rawVersion);
        assertThat(plugin).isNotNull();
        assertThat(plugin.name).isNotBlank().isEqualTo("test");

        var maybePlugin = Optional.ofNullable(rawPlugin)
                .map(p -> PulumiPlugin.fromJson(p.content));
        var maybeVersion = Optional.ofNullable(rawVersion)
                .flatMap(p -> Optionals.ofBlank(p.content));

        var expectedResource = maybePlugin.map(p -> p.resource);
        if (expectedResource.isPresent()) {
            assertThat(plugin.resource).isEqualTo(expectedResource.get());
        } else {
            assertThat(plugin.resource).isFalse();
        }

        var expectedVersion = maybePlugin.map(p -> p.version).or(() -> maybeVersion);
        if (expectedVersion.isPresent()) {
            assertThat(plugin.version).isEqualTo(expectedVersion.get());
        } else {
            assertThat(plugin.version).isNull();
        }

        var expectedServer = maybePlugin.map(p -> p.server);
        if (expectedServer.isPresent()) {
            assertThat(plugin.server).isEqualTo(expectedServer.get());
        } else {
            assertThat(plugin.server).isNull();
        }
    }

    @SuppressWarnings("unused")
    static ArgumentSets testPluginResolverBruteForceSource() {
        return ArgumentSets
                // plugin
                .argumentsForFirstParameter(
                        null,
                        new RawResource("test", "", "{}"),
                        new RawResource("test", "",
                                "{\"resource\": true}"
                        ),
                        new RawResource("test", "",
                                "{\"resource\":true,\"name\":\"test\"}"
                        ),
                        new RawResource("test", "",
                                "{\"resource\":true,\"name\":\"test\",\"version\":\"1.1.1\"}"
                        ),
                        new RawResource("test", "",
                                "{\"resource\":true,\"name\":\"test\",\"version\":\"1.1.1\",\"server\":\"https://example.org\"}"
                        )
                )
                // version
                .argumentsForNextParameter(
                        null,
                        new RawResource("test", "",  ""),
                        new RawResource("test", "", "1.1.1")
                );
    }


    @SuppressWarnings("unused")
    private static Stream<Arguments> testUnmarshalling() {
        return Stream.of(
                arguments(null, null, ""),
                arguments("", null, ""),
                arguments(
                        "{}",
                        new PulumiPlugin(false, null, null, null, null),
                        "{\"resource\":false}"),
                arguments(
                        "{\n" +
                                "  \"resource\":true\n" +
                                "}",
                        new PulumiPlugin(true, null, null, null, null),
                        "{\"resource\":true}"),
                arguments("{\n" +
                                "  \"resource\": true,\n" +
                                "  \"name\": \"unittest\"\n" +
                                "}",
                        new PulumiPlugin(true, "unittest", null, null, null),
                        "{\"resource\":true,\"name\":\"unittest\"}"),
                arguments(
                        "{\n" +
                                "  \"resource\": true,\n" +
                                "  \"name\": \"unittest\",\n" +
                                "  \"version\": \"1.1.1\"\n" +
                                "}",
                        new PulumiPlugin(true, "unittest", "1.1.1", null, null),
                        "{\"resource\":true,\"name\":\"unittest\",\"version\":\"1.1.1\"}"),
                arguments(
                        "{\n" +
                                "  \"resource\": true,\n" +
                                "  \"name\": \"unittest\",\n" +
                                "  \"version\": \"1.1.1\",\n" +
                                "  \"server\": \"https://example.org\"\n" +
                                "}",
                        new PulumiPlugin(true, "unittest", "1.1.1", "https://example.org", null),
                        "{\"resource\":true,\"name\":\"unittest\",\"version\":\"1.1.1\",\"server\":\"https://example.org\"}"),
                arguments(
                        "{\n" +
                                "  \"resource\": true,\n" +
                                "  \"name\": \"unittest\",\n" +
                                "  \"version\": \"1.1.1\",\n" +
                                "  \"parameterization\": {\n" +
                                "    \"name\": \"parameterized\",\n" +
                                "    \"version\": \"2.2.2\",\n" +
                                "    \"value\": \"value\"\n" +
                                "  }\n" +
                                "}",
                        new PulumiPlugin(true, "unittest", "1.1.1", null,
                            new PulumiPluginParameterization("parameterized", "2.2.2", "value")),
                        "{\"resource\":true,\"name\":\"unittest\",\"version\":\"1.1.1\",\"parameterization\":{\"name\":\"parameterized\",\"version\":\"2.2.2\",\"value\":\"value\"}}")
        );
    }

    @ParameterizedTest
    @MethodSource
    void testUnmarshalling(String json, @Nullable PulumiPlugin expected, String expectedJson) {
        var plugin = PulumiPlugin.fromJson(json);
        assertThat(plugin).isEqualTo(expected);
        if (plugin == null) {
            return;
        }
        var gson = new Gson();
        var outputJson = gson.toJson(plugin);
        assertThat(outputJson).isEqualTo(expectedJson);
    }
}
