package com.pulumi.example.kubernetes;

import com.pulumi.Pulumi;
import com.pulumi.kubernetes.core.v1.Namespace;
import com.pulumi.kubernetes.core.v1.NamespaceArgs;
import com.pulumi.kubernetes.helm.v4.Chart;
import com.pulumi.kubernetes.helm.v4.ChartArgs;
import com.pulumi.kubernetes.meta.v1.inputs.ObjectMetaArgs;
import com.pulumi.core.Output;

import java.util.Map;

public class App {
    public static void main(String[] args) {
        Pulumi.run(ctx -> {
                var ns = new Namespace("cert-manager", NamespaceArgs.builder()
                    .metadata(ObjectMetaArgs.builder()
                            .name("cert-manager")
                            .build())
                    .build());

                var certManager = new Chart("cert-manager", ChartArgs.builder()
                    .namespace(ns.metadata().apply(m -> Output.of(m.name().get())))
                    .chart("oci://registry-1.docker.io/bitnamicharts/cert-manager")
                    .version("1.3.1")
                    .build());

            ctx.export("resources", certManager.resources());
        });
    }
}
