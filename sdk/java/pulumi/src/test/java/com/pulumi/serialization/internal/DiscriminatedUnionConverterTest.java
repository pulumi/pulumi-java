package com.pulumi.serialization.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.pulumi.Log;
import com.pulumi.core.TypeShape;
import com.pulumi.core.annotations.CustomType;
import com.pulumi.core.annotations.CustomType.Setter;
import com.pulumi.core.annotations.DiscriminatedUnion;
import com.pulumi.core.internal.Constants;
import com.pulumi.deployment.internal.InMemoryLogger;
import com.pulumi.test.internal.PulumiTestInternal;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.logging.Level;

import static com.pulumi.serialization.internal.ConverterTests.serializeToValueAsync;
import static com.pulumi.test.internal.assertj.PulumiConditions.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscriminatedUnionConverterTest {

    private final static Log log = PulumiTestInternal.mockLog();

    // Basic and Bearer deliberately have an identical property shape, so only dispatch on the
    // discriminator tag can tell them apart.
    @DiscriminatedUnion("discriminantKind")
    @DiscriminatedUnion.Case(tag = "basic", type = BasicAuth.class)
    @DiscriminatedUnion.Case(tag = "bearer", type = BearerAuth.class)
    @DiscriminatedUnion.Case(tag = "apiKey", type = ApiKeyAuth.class)
    public interface AuthConfig {
    }

    @CustomType
    public static final class BasicAuth implements AuthConfig {
        private @Nullable String value;

        @Nullable
        public String value() {
            return value;
        }

        @CustomType.Builder
        public static final class Builder {
            private final BasicAuth $ = new BasicAuth();

            @Setter("value")
            public Builder value(@Nullable String value) {
                this.$.value = value;
                return this;
            }

            public BasicAuth build() {
                return this.$;
            }
        }
    }

    @CustomType
    public static final class BearerAuth implements AuthConfig {
        private @Nullable String value;

        @Nullable
        public String value() {
            return value;
        }

        @CustomType.Builder
        public static final class Builder {
            private final BearerAuth $ = new BearerAuth();

            @Setter("value")
            public Builder value(@Nullable String value) {
                this.$.value = value;
                return this;
            }

            public BearerAuth build() {
                return this.$;
            }
        }
    }

    @CustomType
    public static final class ApiKeyAuth implements AuthConfig {
        private @Nullable String header;
        private @Nullable String value;

        @Nullable
        public String header() {
            return header;
        }

        @Nullable
        public String value() {
            return value;
        }

        @CustomType.Builder
        public static final class Builder {
            private final ApiKeyAuth $ = new ApiKeyAuth();

            @Setter("header")
            public Builder header(@Nullable String header) {
                this.$.header = header;
                return this;
            }

            @Setter("value")
            public Builder value(@Nullable String value) {
                this.$.value = value;
                return this;
            }

            public ApiKeyAuth build() {
                return this.$;
            }
        }
    }

    @CustomType
    public static final class Endpoint {
        private @Nullable String url;
        private @Nullable AuthConfig auth;
        private @Nullable ImmutableList<AuthConfig> fallbacks;

        @Nullable
        public String url() {
            return url;
        }

        @Nullable
        public AuthConfig auth() {
            return auth;
        }

        @Nullable
        public ImmutableList<AuthConfig> fallbacks() {
            return fallbacks;
        }

        @CustomType.Builder
        public static final class Builder {
            private final Endpoint $ = new Endpoint();

            @Setter("url")
            public Builder url(@Nullable String url) {
                this.$.url = url;
                return this;
            }

            @Setter("auth")
            public Builder auth(@Nullable AuthConfig auth) {
                this.$.auth = auth;
                return this;
            }

            @Setter("fallbacks")
            public Builder fallbacks(@Nullable ImmutableList<AuthConfig> fallbacks) {
                this.$.fallbacks = fallbacks;
                return this;
            }

            public Endpoint build() {
                return this.$;
            }
        }
    }

    private static Converter converter() {
        return new Converter(log, new Deserializer(log));
    }

    private static Value secretOf(Value value) {
        return Value.newBuilder().setStructValue(
                Struct.newBuilder()
                        .putFields(Constants.SpecialSigKey,
                                Value.newBuilder().setStringValue(Constants.SpecialSecretSig).build())
                        .putFields(Constants.SecretValueName, value)
                        .build()
        ).build();
    }

    // The whole point of tag dispatch: "basic" is declared first and is structurally identical to
    // "bearer", so a shape-based match would pick the wrong member here.
    @Test
    void testDispatchesOnTagWhenAnotherCaseIsStructurallyIdentical() {
        var serialized = serializeToValueAsync(ImmutableMap.<String, Object>builder()
                .put("discriminantKind", "bearer")
                .put("value", "token")
                .build()).join();

        var data = converter().convertValue("testDispatchesOnTag", serialized, AuthConfig.class);

        assertThat(data.isKnown()).isTrue();
        assertThat(data.getValueNullable()).isInstanceOf(BearerAuth.class);
        assertThat(((BearerAuth) data.getValueNullable()).value()).isEqualTo("token");
    }

    @Test
    void testFirstCaseConverts() {
        var serialized = serializeToValueAsync(ImmutableMap.<String, Object>builder()
                .put("discriminantKind", "basic")
                .put("value", "hunter2")
                .build()).join();

        var data = converter().convertValue("testFirstCaseConverts", serialized, AuthConfig.class);

        assertThat(data.getValueNullable()).isInstanceOf(BasicAuth.class);
        assertThat(((BasicAuth) data.getValueNullable()).value()).isEqualTo("hunter2");
    }

    @Test
    void testCaseWithDistinctShapeConverts() {
        var serialized = serializeToValueAsync(ImmutableMap.<String, Object>builder()
                .put("discriminantKind", "apiKey")
                .put("header", "X-Api-Key")
                .put("value", "key")
                .build()).join();

        var data = converter().convertValue("testCaseWithDistinctShape", serialized, AuthConfig.class);

        assertThat(data.getValueNullable()).isInstanceOf(ApiKeyAuth.class);
        assertThat(((ApiKeyAuth) data.getValueNullable()).header()).isEqualTo("X-Api-Key");
        assertThat(((ApiKeyAuth) data.getValueNullable()).value()).isEqualTo("key");
    }

    @Test
    void testUnionNestedInCustomTypeConverts() {
        var serialized = serializeToValueAsync(ImmutableMap.<String, Object>builder()
                .put("url", "https://example.com")
                .put("auth", ImmutableMap.of("discriminantKind", "bearer", "value", "token"))
                .put("fallbacks", ImmutableList.of(
                        ImmutableMap.of("discriminantKind", "basic", "value", "hunter2"),
                        ImmutableMap.of("discriminantKind", "apiKey", "header", "X-Api-Key", "value", "key")))
                .build()).join();

        var data = converter().convertValue("testNestedUnion", serialized, Endpoint.class).getValueNullable();

        assertThat(data).isNotNull();
        assertThat(data.url()).isEqualTo("https://example.com");
        assertThat(data.auth()).isInstanceOf(BearerAuth.class);
        assertThat(data.fallbacks()).hasSize(2);
        assertThat(data.fallbacks().get(0)).isInstanceOf(BasicAuth.class);
        assertThat(data.fallbacks().get(1)).isInstanceOf(ApiKeyAuth.class);
        assertThat(((ApiKeyAuth) data.fallbacks().get(1)).header()).isEqualTo("X-Api-Key");
    }

    @Test
    void testSecretUnionConverts() {
        var serialized = secretOf(serializeToValueAsync(ImmutableMap.<String, Object>builder()
                .put("discriminantKind", "bearer")
                .put("value", "token")
                .build()).join());

        var data = converter().convertValue("testSecretUnion", serialized, AuthConfig.class);

        assertThat(data.isSecret()).isTrue();
        assertThat(data.getValueNullable()).isInstanceOf(BearerAuth.class);
    }

    @Test
    void testUnknownTagLogs() {
        var logger = InMemoryLogger.getLogger(Level.FINEST, "testUnknownTagLogs");
        var inMemoryLog = PulumiTestInternal.mockLog(logger);
        var converter = new Converter(inMemoryLog, new Deserializer(inMemoryLog));
        var serialized = serializeToValueAsync(ImmutableMap.<String, Object>builder()
                .put("discriminantKind", "oauth")
                .put("value", "token")
                .build()).join();

        var data = converter.convertValue("testUnknownTagLogs", serialized, AuthConfig.class);

        assertThat(data.getValueNullable()).isNull();
        assertThat(logger.getMessages()).haveAtLeastOne(containsString(
                "Unknown 'discriminantKind' value 'oauth'"));
        assertThat(logger.getMessages()).haveAtLeastOne(containsString(
                "expected one of: apiKey, basic, bearer"));
    }

    @Test
    void testMissingDiscriminatorLogs() {
        var logger = InMemoryLogger.getLogger(Level.FINEST, "testMissingDiscriminatorLogs");
        var inMemoryLog = PulumiTestInternal.mockLog(logger);
        var converter = new Converter(inMemoryLog, new Deserializer(inMemoryLog));
        var serialized = serializeToValueAsync(ImmutableMap.<String, Object>of("value", "token")).join();

        var data = converter.convertValue("testMissingDiscriminatorLogs", serialized, AuthConfig.class);

        assertThat(data.getValueNullable()).isNull();
        assertThat(logger.getMessages()).haveAtLeastOne(containsString(
                "Missing discriminator property 'discriminantKind'"));
    }

    // Mirrors the l2-discriminated-union-internal conformance fixture, whose discriminator is named
    // "type__". A trailing double underscore is not an engine-internal name and survives the wire.
    @DiscriminatedUnion("type__")
    @DiscriminatedUnion.Case(tag = "Alpha", type = Alpha.class)
    @DiscriminatedUnion.Case(tag = "Beta", type = Beta.class)
    @DiscriminatedUnion.Case(tag = "Gamma", type = Gamma.class)
    public interface Suffixed {
    }

    @CustomType
    public static final class Alpha implements Suffixed {
        @CustomType.Builder
        public static final class Builder {
            public Alpha build() {
                return new Alpha();
            }
        }
    }

    @CustomType
    public static final class Beta implements Suffixed {
        @CustomType.Builder
        public static final class Builder {
            public Beta build() {
                return new Beta();
            }
        }
    }

    @CustomType
    public static final class Gamma implements Suffixed {
        @CustomType.Builder
        public static final class Builder {
            public Gamma build() {
                return new Gamma();
            }
        }
    }

    @Test
    void testTrailingUnderscoreDiscriminatorConverts() {
        var serialized = serializeToValueAsync(ImmutableMap.<String, Object>of("type__", "Beta")).join();

        var data = converter().convertValue("testTrailingUnderscore", serialized, Suffixed.class);

        assertThat(data.getValueNullable()).isInstanceOf(Beta.class);
    }

    // A leading double underscore marks a property as engine-internal, and Deserializer drops it
    // before the Converter ever sees it. Such a discriminator can therefore never be dispatched on.
    @DiscriminatedUnion("__type")
    @DiscriminatedUnion.Case(tag = "Alpha", type = PrefixedAlpha.class)
    @DiscriminatedUnion.Case(tag = "Beta", type = PrefixedBeta.class)
    @DiscriminatedUnion.Case(tag = "Gamma", type = PrefixedGamma.class)
    public interface Prefixed {
    }

    @CustomType
    public static final class PrefixedAlpha implements Prefixed {
        @CustomType.Builder
        public static final class Builder {
            public PrefixedAlpha build() {
                return new PrefixedAlpha();
            }
        }
    }

    @CustomType
    public static final class PrefixedBeta implements Prefixed {
        @CustomType.Builder
        public static final class Builder {
            public PrefixedBeta build() {
                return new PrefixedBeta();
            }
        }
    }

    @CustomType
    public static final class PrefixedGamma implements Prefixed {
        @CustomType.Builder
        public static final class Builder {
            public PrefixedGamma build() {
                return new PrefixedGamma();
            }
        }
    }

    @Test
    void testLeadingUnderscoreDiscriminatorIsUnreachable() {
        var logger = InMemoryLogger.getLogger(Level.FINEST, "testLeadingUnderscore");
        var inMemoryLog = PulumiTestInternal.mockLog(logger);
        var converter = new Converter(inMemoryLog, new Deserializer(inMemoryLog));
        var serialized = serializeToValueAsync(ImmutableMap.<String, Object>of("__type", "Beta")).join();

        var data = converter.convertValue("testLeadingUnderscore", serialized, Prefixed.class);

        assertThat(data.getValueNullable()).isNull();
        assertThat(logger.getMessages()).haveAtLeastOne(containsString(
                "Missing discriminator property '__type'"));
    }

    @DiscriminatedUnion("kind")
    @DiscriminatedUnion.Case(tag = "stray", type = BasicAuth.class)
    public interface Mismatched {
    }

    @Test
    void testCaseNotImplementingTheUnionIsRejected() {
        assertThatThrownBy(() -> converter().checkTargetType("testMismatched", TypeShape.of(Mismatched.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("case 'stray'")
                .hasMessageContaining("to implement");
    }

    @DiscriminatedUnion("kind")
    public interface NoCases {
    }

    @Test
    void testUnionWithoutCasesIsRejected() {
        assertThatThrownBy(() -> converter().checkTargetType("testNoCases", TypeShape.of(NoCases.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("to also carry at least one @Case");
    }
}
