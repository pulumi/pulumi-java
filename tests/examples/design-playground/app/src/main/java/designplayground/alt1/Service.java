package designplayground.alt1;

import io.pulumi.core.Output;

import java.util.function.Function;

public final class Service {
    public Service(String name, ServiceArgs args) {
    }

    public Service(String name, Function<ServiceArgs.Builder, ServiceArgs.Builder> f) {
        this(name, f.apply(ServiceArgs.builder()).build());
    }
    public Output<String> getApiVersion() {
        return null;
    }
}