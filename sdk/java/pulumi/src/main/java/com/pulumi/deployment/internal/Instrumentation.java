package com.pulumi.deployment.internal;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages OpenTelemetry tracing initialization for the Pulumi Java SDK.
 * When TRACEPARENT is set in the environment, this class initializes OTel
 * with an OTLP exporter and gRPC client instrumentation.
 */
public final class Instrumentation {

    private static final Logger logger = Logger.getLogger(Instrumentation.class.getName());
    private static OpenTelemetrySdk sdk;
    private static Span rootSpan;
    private static Scope rootScope;
    private static ClientInterceptor clientInterceptor;

    private Instrumentation() {}

    /**
     * Initialize OpenTelemetry tracing if TRACEPARENT is set.
     */
    public static void initialize() {
        var traceparent = System.getenv("TRACEPARENT");
        if (traceparent == null || traceparent.isEmpty()) {
            return;
        }

        try {
            var resourceBuilder = Resource.getDefault().toBuilder()
                    .put("service.name", "pulumi-sdk-java")
                    .build();

            var tracerProviderBuilder = SdkTracerProvider.builder()
                    .setResource(resourceBuilder);

            var otlpEndpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT");
            if (otlpEndpoint != null && !otlpEndpoint.isEmpty()) {
                var exporter = OtlpGrpcSpanExporter.builder()
                        .setEndpoint("http://" + otlpEndpoint)
                        .build();
                tracerProviderBuilder.addSpanProcessor(BatchSpanProcessor.builder(exporter).build());
            }

            sdk = OpenTelemetrySdk.builder()
                    .setTracerProvider(tracerProviderBuilder.build())
                    .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                    .build();

            Map<String, String> carrier = new HashMap<>();
            carrier.put("traceparent", traceparent);

            Context extractedContext = W3CTraceContextPropagator.getInstance().extract(
                    Context.root(), carrier, MapTextMapGetter.INSTANCE);

            Tracer tracer = sdk.getTracer("pulumi-sdk-java");
            rootSpan = tracer.spanBuilder("pulumi-sdk-java")
                    .setParent(extractedContext)
                    .startSpan();

            Context rootContext = extractedContext.with(rootSpan);
            rootScope = rootContext.makeCurrent();

            var grpcTelemetry = GrpcTelemetry.create(sdk);
            var otelInterceptor = grpcTelemetry.newClientInterceptor();
            clientInterceptor = new ClientInterceptor() {
                @Override
                public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                        MethodDescriptor<ReqT, RespT> method,
                        CallOptions callOptions,
                        Channel next) {
                    try (Scope scope = rootContext.makeCurrent()) {
                        return otelInterceptor.interceptCall(method, callOptions, next);
                    }
                }
            };

            logger.log(Level.FINE, "OpenTelemetry tracing initialized with TRACEPARENT: " + traceparent);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to initialize OpenTelemetry tracing", e);
        }
    }

    /**
     * Returns the gRPC client interceptor for OTel instrumentation, or null if tracing is not enabled.
     */
    @Nullable
    public static ClientInterceptor getClientInterceptor() {
        return clientInterceptor;
    }

    /**
     * Shutdown the OTel SDK and flush any pending spans.
     */
    public static void shutdown() {
        if (rootScope != null) {
            rootScope.close();
            rootScope = null;
        }
        if (rootSpan != null) {
            rootSpan.end();
            rootSpan = null;
        }
        if (sdk != null) {
            sdk.close();
            sdk = null;
        }
        clientInterceptor = null;
    }

    private enum MapTextMapGetter implements TextMapGetter<Map<String, String>> {
        INSTANCE;

        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Nullable
        @Override
        public String get(@Nullable Map<String, String> carrier, String key) {
            if (carrier == null) {
                return null;
            }
            return carrier.get(key);
        }
    }
}
