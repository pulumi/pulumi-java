package io.pulumi.core.function;

// FIXME: some core c# interface, will probably not be needed, not in this form at lest,
//        looks like some kind of injection
public interface ServiceProvider {
    Object getService(Class<?> serviceType);
}
