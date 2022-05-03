package com.broom.example;

import com.pulumi.Context;
import com.pulumi.Pulumi;

import java.util.List;

public class PulumiStack {
    public static void main(String[] args) {
        Pulumi.run(PulumiStack::stack);
    }
    public static void stack(Context ctx) {
        final var s3Stack = new S3Stack();
        final var k8sStack = new EKSStack(List.of(s3Stack.readPolicy()));
        ctx.export("kubeconfig", k8sStack.kubeconfig().asSecret());
        ctx.export("bucketName", s3Stack.bucketName());
    }
}
