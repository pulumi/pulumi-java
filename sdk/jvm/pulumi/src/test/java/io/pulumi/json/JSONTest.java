package io.pulumi.json;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class JSONTest {

    @SuppressWarnings("unused")
    private static Stream<Arguments> testJSON() {
        return Stream.of(
                arguments(
                        JSON.object(o -> o.null_("null")), "{\"null\":null}"
                ),
                arguments(
                        JSON.array(JSONArray::null_), "[null]"
                ),
                arguments(
                        JSON.array(), "[]"
                ),
                arguments(
                        JSON.object(), "{}"
                ),
                arguments(
                        JSON.array(), "[]"
                ),
                arguments(
                        JSON.object(j -> {
                            j.string("Version", "2012-10-17");
                            j.array("Statement", s -> s.object(so -> {
                                so.string("Effect", "Allow");
                                so.string("Principal", "*");
                                so.array("Action", a -> a.string("s3:GetObject"));
                                so.array("Resource", a -> a.string("bucketArn/*"));
                            }));
                        }),
                        "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\"," +
                                "\"Principal\":\"*\",\"Action\":[\"s3:GetObject\"],\"Resource\":[\"bucketArn/*\"]}]}"
                ),
                arguments(
                        JSON.array(j -> j.object(so -> {
                            so.string("Effect", "Allow");
                            so.string("Principal", "*");
                            so.array("Action", a -> a.string("s3:GetObject"));
                            so.array("Resource", a -> a.string("bucketArn/*"));
                        })),
                        "[{\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":[\"s3:GetObject\"]," +
                                "\"Resource\":[\"bucketArn/*\"]}]"
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testJSON(JSONElement json, String expected) {
        assertThat(json.asString()).isEqualTo(expected);
    }
}