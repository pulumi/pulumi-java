package com.pulumi.provider.internal;

import com.google.protobuf.Empty;
import com.pulumi.resources.ResourceArgs;
import com.pulumi.serialization.internal.PolicyPackages;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import pulumirpc.AnalyzerOuterClass;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AnalyzerService {
    private static final Logger logger = Logger.getLogger(AnalyzerService.class.getName());

    Server server; // Exposed as private-package for testing.
    private final String engineAddress;

    public AnalyzerService(String engineAddress) {
        this.engineAddress = engineAddress;
    }

    public void startAndBlockUntilShutdown() throws IOException, InterruptedException {
        start();
        blockUntilShutdown();
    }

    void start() throws IOException {
        server = createServerBuilder()
                .addService(new AnalyzerImpl(this.engineAddress))
                .intercept(new ErrorHandlingInterceptor())
                .build()
                .start();

        Logger.getLogger("io.grpc.netty.shaded.io.grpc.netty.NettyServerHandler").setLevel(Level.SEVERE);

        // Print the actual bound port for the parent process to read
        System.out.println(server.getPort());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                AnalyzerService.this.stop();
            } catch (InterruptedException e) {
                logger.severe(e.toString());
            }
        }));
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    protected ServerBuilder<?> createServerBuilder() {
        return ServerBuilder.forPort(0);
    }

    static class AnalyzerImpl extends pulumirpc.AnalyzerGrpc.AnalyzerImplBase {
        private final String engineAddress;

        public AnalyzerImpl(String engineAddress) {
            this.engineAddress = engineAddress;
        }

        @Override
        public void getPluginInfo(Empty request, StreamObserver<pulumirpc.Plugin.PluginInfo> responseObserver) {
            // Return basic plugin information
            pulumirpc.Plugin.PluginInfo info = pulumirpc.Plugin.PluginInfo.newBuilder()
                    .setVersion("1.0.0")
                    .build();

            responseObserver.onNext(info);
            responseObserver.onCompleted();
        }

        @Override
        public void getAnalyzerInfo(Empty request,
                                    StreamObserver<AnalyzerOuterClass.AnalyzerInfo> responseObserver) {
            for (var pack : PolicyPackages.get()) {
                var builder = AnalyzerOuterClass.AnalyzerInfo.newBuilder()
                        .setName(pack.annotation.name())
                        .setVersion("1.0.0");

                for (var entry : pack.policies.entrySet()) {
                    var key = entry.getKey();
                    var value = entry.getValue();

                    var subBuilder = pulumirpc.AnalyzerOuterClass.PolicyInfo.newBuilder()
                            .setName(value.annotation.name())
                            .setDescription(value.annotation.description())
                            .setEnforcementLevelValue(value.annotation.enforcementLevel().ordinal());

                    builder.addPolicies(subBuilder);
                }
                responseObserver.onNext(builder.build());
            }

            responseObserver.onCompleted();
        }

        @Override
        public void configure(AnalyzerOuterClass.ConfigureAnalyzerRequest request,
                              StreamObserver<Empty> responseObserver) {
            responseObserver.onNext(null);
            responseObserver.onCompleted();
        }

        @Override
        public void analyzeStack(AnalyzerOuterClass.AnalyzeStackRequest request,
                                 StreamObserver<AnalyzerOuterClass.AnalyzeResponse> responseObserver) {
            responseObserver.onNext(null);
            responseObserver.onCompleted();
        }

        @Override
        public void analyze(AnalyzerOuterClass.AnalyzeRequest request,
                            StreamObserver<AnalyzerOuterClass.AnalyzeResponse> responseObserver) {
            var builder = AnalyzerOuterClass.AnalyzeResponse.newBuilder();

            String resourceType = request.getType();

            for (var pack : PolicyPackages.get()) {
                var policy = pack.policies.get(resourceType);
                if (policy != null) {
//                    System.err.printf("Found handler: %s\n", policy.target);

                    try {
                        var resourceArgs = ResourceArgs.deserialize(request.getProperties(), policy.resourceArgsClass);

                        var violations = new ArrayList<String>();
                        policy.target.invoke(null, resourceArgs, violations);

                        for (var violation : violations) {
                            var diag = pulumirpc.AnalyzerOuterClass.AnalyzeDiagnostic.newBuilder()
                                    .setEnforcementLevel(policy.annotation.enforcementLevel())
                                    .setPolicyPackName(pack.annotation.name())
                                    .setPolicyPackVersion("1.0.0")
                                    .setPolicyName(policy.annotation.name())
                                    .setDescription(policy.annotation.description())
                                    .setUrn(request.getUrn())
                                    .setMessage(violation).build();
                            builder.addDiagnostics(diag);
                        }
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void remediate(AnalyzerOuterClass.AnalyzeRequest request,
                              StreamObserver<AnalyzerOuterClass.RemediateResponse> responseObserver) {
            responseObserver.onNext(AnalyzerOuterClass.RemediateResponse.newBuilder().build());
            responseObserver.onCompleted();
        }
    }
} 
