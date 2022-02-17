package io.pulumi.serialization.internal;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import io.pulumi.Log;
import io.pulumi.core.Input;
import io.pulumi.core.internal.Environment;
import io.pulumi.core.internal.annotations.InputImport;
import io.pulumi.deployment.internal.EngineLogger;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertiesSerializerTests {
    @Test
    void regress165() {
        var log = new Log(EngineLogger.ignore());
        var providerArgs = ProviderArgs.builder().setKubeconfig("k8s-config").build();
        var label = "LABEL";
        var keepResources = true;

        var s = new PropertiesSerializer(log);
        try {
            var args = providerArgs.internalToOptionalMapAsync(log).get();
            Struct struct = s.serializeAllPropertiesAsync(label, args, keepResources).get();
            // NOTE: double-" encoded because json=true.
            assertThat(showStruct(struct)).isEqualTo("{\"kubeconfig\":\"\\\"k8s-config\\\"\"}");
        } catch (Exception e) {
            System.out.println(e.getStackTrace());
            assert e == null;
        }
    }

    private static String showStruct(Struct struct) {
        try {
            return JsonFormat.printer()
                    .omittingInsignificantWhitespace()
                    .sortingMapKeys()
                    .print(struct);
        } catch (InvalidProtocolBufferException e) {
            return "ERROR";
        }
    }
}

class Utilities {

    public static Optional<String> getEnv(String... names) {
        for (var n : names) {
            var value = Environment.getEnvironmentVariable(n);
            if (value.isValue()) {
                return Optional.of(value.value());
            }
        }
        return Optional.empty();
    }
}

class ProviderArgs extends io.pulumi.resources.ResourceArgs {

    public static final ProviderArgs Empty = new ProviderArgs();

    @InputImport(name = "kubeconfig", json = true)
    private final @Nullable
    Input<String> kubeconfig;

    public Input<String> getKubeconfig() {
        return this.kubeconfig == null ? Input.empty() : this.kubeconfig;
    }

    @InputImport(name = "renderYamlToDirectory", json = true)
    private final @Nullable
    Input<String> renderYamlToDirectory;

    public Input<String> getRenderYamlToDirectory() {
        return this.renderYamlToDirectory == null ? Input.empty() : this.renderYamlToDirectory;
    }

    public ProviderArgs(
            @Nullable Input<String> kubeconfig,
            @Nullable Input<String> renderYamlToDirectory) {
        this.kubeconfig = kubeconfig == null ? Input.ofNullable(Utilities.getEnv("KUBECONFIG").orElse(null)) : kubeconfig;
        this.renderYamlToDirectory = renderYamlToDirectory;
    }

    private ProviderArgs() {
        this.kubeconfig = Input.empty();
        this.renderYamlToDirectory = Input.empty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ProviderArgs defaults) {
        return new Builder(defaults);
    }

    public static final class Builder {
        private @Nullable
        Input<String> kubeconfig;
        private @Nullable
        Input<String> renderYamlToDirectory;

        public Builder() {
            // Empty
        }

        public Builder(ProviderArgs defaults) {
            Objects.requireNonNull(defaults);
            this.kubeconfig = defaults.kubeconfig;
            this.renderYamlToDirectory = defaults.renderYamlToDirectory;
        }

        public Builder setKubeconfig(@Nullable Input<String> kubeconfig) {
            this.kubeconfig = kubeconfig;
            return this;
        }

        public Builder setKubeconfig(@Nullable String kubeconfig) {
            this.kubeconfig = Input.ofNullable(kubeconfig);
            return this;
        }

        public Builder setRenderYamlToDirectory(@Nullable Input<String> renderYamlToDirectory) {
            this.renderYamlToDirectory = renderYamlToDirectory;
            return this;
        }

        public Builder setRenderYamlToDirectory(@Nullable String renderYamlToDirectory) {
            this.renderYamlToDirectory = Input.ofNullable(renderYamlToDirectory);
            return this;
        }

        public ProviderArgs build() {
            return new ProviderArgs(kubeconfig, renderYamlToDirectory);
        }
    }
}


