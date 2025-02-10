// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Empty;
import com.pulumi.deployment.internal.DeploymentImpl;
import com.pulumi.deployment.internal.InlineDeploymentSettings;
import com.pulumi.internal.PulumiInternal;
import com.pulumi.resources.StackOptions;
import com.pulumi.Context;
import io.grpc.stub.StreamObserver;
import pulumirpc.LanguageRuntimeGrpc.LanguageRuntimeImplBase;
import pulumirpc.Plugin.PluginInfo;
import pulumirpc.Language.GetRequiredPackagesRequest;
import pulumirpc.Language.GetRequiredPackagesResponse;
import pulumirpc.Language.RunRequest;
import pulumirpc.Language.RunResponse;

/**
 * Internal implementation of the LanguageRuntime service.
 */
final class LanguageRuntimeImpl extends LanguageRuntimeImplBase {
    private static final Semaphore semaphore = new Semaphore(1);

    private final Consumer<Context> program;
    private final Logger logger;

    public LanguageRuntimeImpl(Consumer<Context> program, Logger logger) {
        this.program = program;
        this.logger = logger;
    }

    @Override
    public void getRequiredPackages(GetRequiredPackagesRequest request,
            StreamObserver<GetRequiredPackagesResponse> responseObserver) {
        var response = GetRequiredPackagesResponse.newBuilder()
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void run(RunRequest request,
            StreamObserver<RunResponse> responseObserver) {
        try {
            var args = request.getArgsList();
            var engineAddress = args != null && !args.isEmpty() ? args.get(0) : "";

            var inlineDeploymentSettings = InlineDeploymentSettings.builder()
                    // .logger(logger) // TODO pass the logger
                    .engineAddr(engineAddress)
                    .monitorAddr(request.getMonitorAddress())
                    .config(ImmutableMap.copyOf(request.getConfigMap()))
                    .configSecretKeys(ImmutableSet.copyOf(request.getConfigSecretKeysList()))
                    .organization(request.getOrganization())
                    .project(request.getProject())
                    .stack(request.getStack())
                    .isDryRun(request.getDryRun())
                    .build();

            // TODO Remove lock once https://github.com/pulumi/pulumi-java/issues/30 is
            // resolved.
            semaphore.acquire();
            try {
                var pulumiInternal = PulumiInternal.fromInline(inlineDeploymentSettings, StackOptions.Empty);
                pulumiInternal.runAsync(program).handle((result, throwable) -> {
                    try {
                        var responseBuilder = RunResponse.newBuilder();
                        if (throwable != null) {
                            responseBuilder.setError(throwable.getMessage());
                        }
                        responseObserver.onNext(responseBuilder.build());
                        responseObserver.onCompleted();
                        return null;
                    } finally {
                        DeploymentImpl.internalUnsafeDestroyInstance();
                        semaphore.release();
                    }
                });
            } catch (Exception e) {
                DeploymentImpl.internalUnsafeDestroyInstance();
                semaphore.release();
                throw e;
            }

            // TODO graceful error propagation/handling

        } catch (Exception e) {
            String errorDetails = getDetailedErrorMessage(e, "Run failed");
            responseObserver.onError(io.grpc.Status.UNKNOWN
                    .withDescription(errorDetails)
                    .withCause(e)
                    .asException());
        }
    }

    @Override
    public void getPluginInfo(Empty request, StreamObserver<PluginInfo> responseObserver) {
        var info = PluginInfo.newBuilder()
                .setVersion("1.0.0")
                .build();
        responseObserver.onNext(info);
        responseObserver.onCompleted();
    }

    private static String getDetailedErrorMessage(Throwable t, String context) {
        StringBuilder errorDetails = new StringBuilder();
        errorDetails.append(context).append(":\n");
        errorDetails.append("Exception class: ").append(t.getClass().getName()).append("\n");
        errorDetails.append("Message: ").append(t.getMessage() != null ? t.getMessage() : "No message").append("\n");
        errorDetails.append("Stack trace:\n");
        for (StackTraceElement element : t.getStackTrace()) {
            errorDetails.append("\tat ").append(element.toString()).append("\n");
        }

        Throwable cause = t.getCause();
        if (cause != null) {
            errorDetails.append("Caused by: ").append(cause.getClass().getName()).append(": ")
                    .append(cause.getMessage() != null ? cause.getMessage() : "No message").append("\n");
            for (StackTraceElement element : cause.getStackTrace()) {
                errorDetails.append("\tat ").append(element.toString()).append("\n");
            }
        }
        return errorDetails.toString();
    }
}
