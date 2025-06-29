package com.pulumi.provider.internal;

import io.grpc.*;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A gRPC {@link ServerInterceptor} that catches and reports uncaught exceptions as internal errors.
 *
 * @see io.grpc.ServerInterceptor
 * @see io.grpc.Status
 */
public class ErrorHandlingInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            io.grpc.Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {}, headers)) {
            @Override
            public void onHalfClose() {
                try {
                    super.onHalfClose();
                } catch (Throwable e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    
                    StringWriter sw = new StringWriter();
                    cause.printStackTrace(new PrintWriter(sw));
                    String stackTrace = sw.toString();
                    
                    Status status = Status.INTERNAL
                        .withDescription(String.format("%s: %s\n%s", 
                            cause.getClass().getName(),
                            cause.getMessage(),
                            stackTrace));
                            
                    call.close(status, new io.grpc.Metadata());
                }
            }
        };
    }
} 