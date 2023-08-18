package com.pulumi.automation.internal;

import io.grpc.stub.StreamObserver;
import pulumirpc.Language.RunRequest;
import pulumirpc.Language.RunResponse;
import pulumirpc.LanguageRuntimeGrpc;

import javax.annotation.ParametersAreNonnullByDefault;
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

        var requestContext = new RunRequestContext(
                engineAddress,
                request.getMonitorAddress(),
                request.getConfigMap(),
                request.getConfigSecretKeysList(),
                request.getProject(),
                request.getStack(),
                request.getDryRun()
        );

        try (var pulumi = PulumiAutoInternal.from(this.logger, requestContext)) {
            pulumi.runAutoAsync(context.program()).join();
        }
    }
}
