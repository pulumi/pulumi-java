package designplayground.alt1;

public final class ServicePortArgs {
    public class Builder {
        public Builder port(int newPort) {
            return this;
        }

        public Builder targetPort(String newTargetPort) {
            return this;
        }

        public ServicePortArgs build() {
            return null;
        }
    }

    public static Builder builder() {
        return builder();
    }
}