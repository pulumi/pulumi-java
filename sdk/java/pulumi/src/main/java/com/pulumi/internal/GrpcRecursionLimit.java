package com.pulumi.internal;

import com.google.protobuf.Message;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;
import io.grpc.MethodDescriptor.Marshaller;
import io.grpc.MethodDescriptor.PrototypeMarshaller;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import io.grpc.protobuf.ProtoUtils;

import java.util.ArrayList;

/**
 * Raises the nesting limit protobuf enforces when Pulumi's gRPC messages are parsed.
 *
 * <p>Resource inputs and outputs travel as protobuf {@code Struct}s, and each level of a
 * user-visible object costs three levels of protobuf nesting: {@code Struct}, map entry,
 * {@code Value}. Protobuf's Java runtime refuses to parse past 100 levels, and gRPC's default
 * marshaller keeps that default, so a resource whose schema nests deeply - AWS' recursive
 * JSON Schema shapes, for instance - fails to deserialize. The failure lands on the channel
 * rather than the resource, which aborts the whole program instead of one registration.
 *
 * <p>The marshallers installed here parse at {@link #RECURSION_LIMIT} instead. Serialization is
 * left alone: protobuf imposes no depth limit when writing.
 */
public final class GrpcRecursionLimit {

    /**
     * Matches the limit the Go engine parses with, so that the Java SDK is not the narrower end
     * of the connection. Well above what the JVM can reach in practice: protobuf parses
     * recursively, so the thread's stack runs out first, somewhere past 300 levels of property
     * nesting. That makes this a ceiling on the artificial limit rather than on the payload.
     */
    public static final int RECURSION_LIMIT = 10_000;

    private static final ClientInterceptor CLIENT_INTERCEPTOR = new ClientInterceptor() {
        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
            return next.newCall(
                    method.toBuilder()
                            .setResponseMarshaller(raise(method.getResponseMarshaller()))
                            .build(),
                    callOptions);
        }
    };

    private GrpcRecursionLimit() {}

    /**
     * Returns a channel interceptor that parses responses at {@link #RECURSION_LIMIT}.
     *
     * @return the interceptor, safe to share between channels
     */
    public static ClientInterceptor clientInterceptor() {
        return CLIENT_INTERCEPTOR;
    }

    /**
     * Returns a copy of {@code definition} whose methods parse requests at
     * {@link #RECURSION_LIMIT}. A {@code ServerInterceptor} cannot do this, because the method
     * descriptor has already deserialized the request by the time one is consulted.
     *
     * @param definition the service to rebind
     * @return the rebound service, to hand to {@code ServerBuilder.addService}
     */
    public static ServerServiceDefinition intercept(ServerServiceDefinition definition) {
        var original = definition.getServiceDescriptor();
        var descriptorBuilder = ServiceDescriptor.newBuilder(original.getName())
                .setSchemaDescriptor(original.getSchemaDescriptor());
        var methods = new ArrayList<ServerMethodDefinition<?, ?>>();
        for (var method : definition.getMethods()) {
            var raised = raiseRequestMarshaller(method);
            methods.add(raised);
            descriptorBuilder.addMethod(raised.getMethodDescriptor());
        }
        var builder = ServerServiceDefinition.builder(descriptorBuilder.build());
        for (var method : methods) {
            builder.addMethod(method);
        }
        return builder.build();
    }

    private static <ReqT, RespT> ServerMethodDefinition<ReqT, RespT> raiseRequestMarshaller(
            ServerMethodDefinition<ReqT, RespT> method) {
        var descriptor = method.getMethodDescriptor();
        return ServerMethodDefinition.create(
                descriptor.toBuilder()
                        .setRequestMarshaller(raise(descriptor.getRequestMarshaller()))
                        .build(),
                method.getServerCallHandler());
    }

    /**
     * Rebuilds a generated protobuf marshaller with the raised limit, or returns it untouched if
     * it does not expose the prototype needed to do so.
     */
    @SuppressWarnings("unchecked")
    private static <T> Marshaller<T> raise(Marshaller<T> marshaller) {
        if (!(marshaller instanceof PrototypeMarshaller)) {
            return marshaller;
        }
        var prototype = ((PrototypeMarshaller<T>) marshaller).getMessagePrototype();
        if (!(prototype instanceof Message)) {
            return marshaller;
        }
        return (Marshaller<T>) ProtoUtils.marshallerWithRecursionLimit(
                (Message) prototype, RECURSION_LIMIT);
    }
}
