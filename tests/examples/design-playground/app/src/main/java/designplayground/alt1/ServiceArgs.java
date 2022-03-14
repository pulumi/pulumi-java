package designplayground.alt1;

import io.pulumi.core.Output;

import java.util.function.Function;

public final class ServiceArgs {

    public static Builder builder() {
        return null;
    }

    public class Builder {

        public ServiceArgs build() {
            return null;
        }

        public Builder metadata(Output<ObjectMetaArgs> metadataNew) {
            return this;
        }

        public Builder metadata(ObjectMetaArgs metadataNew) {
            return metadata(Output.of(metadataNew));
        }

        public Builder metadata(Function<ObjectMetaArgs.Builder,ObjectMetaArgs.Builder> f) {
            return metadata(f.apply(ObjectMetaArgs.builder()).build());
        }

        public Builder spec(Output<ServiceSpecArgs> specNew) {
            return this;
        }

        public Builder spec(ServiceSpecArgs specNew) {
            return spec(Output.of(specNew));
        }

        public Builder spec(Function<ServiceSpecArgs.Builder,ServiceSpecArgs.Builder> builder) {
            return spec(builder.apply(ServiceSpecArgs.builder()).build());
        }
    }
}
