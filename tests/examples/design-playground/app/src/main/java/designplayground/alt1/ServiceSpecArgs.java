package designplayground.alt1;

import io.pulumi.core.Either;
import io.pulumi.core.Input;

import java.util.List;

public final class ServiceSpecArgs {

    public static Builder builder() {
        return null;
    }

    public class Builder {

        public ServiceSpecArgs build() {
            return null;
        }

        public Builder type(Input<Either<String, ServiceSpecType>> typeNew) {
            return this;
        }

        public Builder type(ServiceSpecType typeNew) {
            return type(Input.of(Either.ofRight(typeNew)));
        }

        public Builder ports(Input<List<ServicePortArgs>> portsNew) {
            return this;
        }

        public Builder ports(List<ServicePortArgs> portsNew) {
            return ports(Input.of(portsNew));
        }
    }
}