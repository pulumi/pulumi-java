package com.broom.example;

import com.pulumi.Context;
import com.pulumi.Exports;
import com.pulumi.Pulumi;

import java.util.List;

public class PulumiStack {
    public static void main(String[] args) {
        int exitCode = Pulumi.run(PulumiStack::stack);
        System.exit(exitCode);
    }

    private static Exports stack(Context ctx) {
        final var s3Stack = new S3Stack();
        final var k8sStack = new EKSStack(List.of(s3Stack.readPolicy()));
        ctx.export("kubeconfig", k8sStack.kubeconfig().asSecret());
        ctx.export("bucketName", s3Stack.bucketName());
        return ctx.exports();
    }
}
