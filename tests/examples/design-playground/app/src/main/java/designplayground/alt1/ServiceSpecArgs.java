package designplayground.alt1;

import io.pulumi.core.Either;
import io.pulumi.core.Output;

import java.util.List;

public final class ServiceSpecArgs {

    public static Builder builder() {
        return null;
    }

    public class Builder {

        public ServiceSpecArgs build() {
            return null;
        }

        public Builder type(Output<Either<String, ServiceSpecType>> typeNew) {
            return this;
        }

        public Builder type(ServiceSpecType typeNew) {
            return type(Output.of(Either.ofRight(typeNew)));
        }

        public Builder ports(Output<List<ServicePortArgs>> portsNew) {
            return this;
        }

        public Builder ports(List<ServicePortArgs> portsNew) {
            return ports(Output.of(portsNew));
        }
    }
}
