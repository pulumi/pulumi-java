package com.pulumi.automation.internal;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.pulumi.Config;
import com.pulumi.context.internal.ConfigContextInternal;
import com.pulumi.context.internal.ContextInternal;
import com.pulumi.context.internal.LoggingContextInternal;
import com.pulumi.context.internal.OutputContextInternal;
import com.pulumi.core.internal.OutputFactory;
import com.pulumi.deployment.Deployment;
import com.pulumi.deployment.internal.DeploymentImpl;
import com.pulumi.deployment.internal.DeploymentInstanceHolder;
import com.pulumi.deployment.internal.DeploymentInstanceInternal;
import com.pulumi.deployment.internal.GrpcEngine;
import com.pulumi.deployment.internal.GrpcMonitor;
import io.grpc.stub.StreamObserver;
import pulumirpc.Language.RunRequest;
import pulumirpc.Language.RunResponse;
import pulumirpc.LanguageRuntimeGrpc;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

@ParametersAreNonnullByDefault
public class LanguageRuntimeService extends LanguageRuntimeGrpc.LanguageRuntimeImplBase {

    private final Logger logger;
    private final LanguageRuntimeContext context;

    public LanguageRuntimeService(Logger logger, LanguageRuntimeContext context) {
        this.logger = requireNonNull(logger);
        this.context = requireNonNull(context);
    }

    @Override
    public void run(RunRequest request, StreamObserver<RunResponse> responseObserver) {
        this.logger.finest(String.format("request: %s", request));
        try {
            run(request);
        } catch (Exception e) {
            responseObserver.onError(e);
            throw e;
        }
        responseObserver.onNext(RunResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    private void run(RunRequest request) {
        var args = request.getArgsList();
        var engineAddress = args.size() > 0 ? args.get(0) : "";
        var engine = new GrpcEngine(engineAddress);
        var monitor = new GrpcMonitor(request.getMonitorAddress());

        var conf = new DeploymentImpl.Config(
                ImmutableMap.copyOf(request.getConfigMap()),
                ImmutableSet.copyOf(request.getConfigSecretKeysList())
        );

        var projectName = request.getProject();
        var stackName = request.getStack();
        var dryRun = request.getDryRun();

        var state = new DeploymentImpl.DeploymentState(conf, logger, projectName, stackName, dryRun, engine, monitor);
        var deployment = new DeploymentImpl(state);
        DeploymentInstanceHolder.setInstance(new DeploymentInstanceInternal(deployment));

        var instance = Deployment.getInstance();
        var runner = deployment.getRunner();
        var log = deployment.getLog();

        Function<String, Config> configFactory = (name) -> new Config(instance.getConfig(), name);
        var config = new ConfigContextInternal(projectName, configFactory);
        var logging = new LoggingContextInternal(log);
        var outputFactory = new OutputFactory(runner);
        var outputs = new OutputContextInternal(outputFactory);
        var ctx = new ContextInternal(
                projectName, stackName, logging, config, outputs, List.of()
        );
        try (var pulumi = new PulumiAutoInternal(runner, ctx)) {
            pulumi.runAutoAsync(context.program()).join();
        }
    }
}
