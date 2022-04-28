package generated_program;

import java.util.*;
import java.io.*;
import java.nio.*;
import com.pulumi.*;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static Exports stack(Context ctx) {
        var pulumi_kubernetes_operatorDeployment = new Deployment("pulumi_kubernetes_operatorDeployment", DeploymentArgs.builder()        
            .apiVersion("apps/v1")
            .kind("Deployment")
            .metadata(ObjectMeta.builder()
                .name("pulumi-kubernetes-operator")
                .build())
            .spec(DeploymentSpec.builder()
                .replicas(1)
                .selector(LabelSelector.builder()
                    .matchLabels(Map.of("name", "pulumi-kubernetes-operator"))
                    .build())
                .template(PodTemplateSpec.builder()
                    .metadata(ObjectMeta.builder()
                        .labels(Map.of("name", "pulumi-kubernetes-operator"))
                        .build())
                    .spec(PodSpec.builder()
                        .serviceAccountName("pulumi-kubernetes-operator")
                        .imagePullSecrets(LocalObjectReference.builder()
                            .name("pulumi-kubernetes-operator")
                            .build())
                        .containers(Container.builder()
                            .name("pulumi-kubernetes-operator")
                            .image("pulumi/pulumi-kubernetes-operator:v0.0.2")
                            .command("pulumi-kubernetes-operator")
                            .args("--zap-level=debug")
                            .imagePullPolicy("Always")
                            .env(                            
                                EnvVar.builder()
                                    .name("WATCH_NAMESPACE")
                                    .valueFrom(EnvVarSource.builder()
                                        .fieldRef(ObjectFieldSelector.builder()
                                            .fieldPath("metadata.namespace")
                                            .build())
                                        .build())
                                    .build(),
                                EnvVar.builder()
                                    .name("POD_NAME")
                                    .valueFrom(EnvVarSource.builder()
                                        .fieldRef(ObjectFieldSelector.builder()
                                            .fieldPath("metadata.name")
                                            .build())
                                        .build())
                                    .build(),
                                EnvVar.builder()
                                    .name("OPERATOR_NAME")
                                    .value("pulumi-kubernetes-operator")
                                    .build())
                            .build())
                        .build())
                    .build())
                .build())
            .build());

        var pulumi_kubernetes_operatorRole = new Role("pulumi_kubernetes_operatorRole", RoleArgs.builder()        
            .apiVersion("rbac.authorization.k8s.io/v1")
            .kind("Role")
            .metadata(ObjectMeta.builder()
                .creationTimestamp(null)
                .name("pulumi-kubernetes-operator")
                .build())
            .rules(            
                PolicyRule.builder()
                    .apiGroups("")
                    .resources(                    
                        "pods",
                        "services",
                        "services/finalizers",
                        "endpoints",
                        "persistentvolumeclaims",
                        "events",
                        "configmaps",
                        "secrets")
                    .verbs(                    
                        "create",
                        "delete",
                        "get",
                        "list",
                        "patch",
                        "update",
                        "watch")
                    .build(),
                PolicyRule.builder()
                    .apiGroups("apps")
                    .resources(                    
                        "deployments",
                        "daemonsets",
                        "replicasets",
                        "statefulsets")
                    .verbs(                    
                        "create",
                        "delete",
                        "get",
                        "list",
                        "patch",
                        "update",
                        "watch")
                    .build(),
                PolicyRule.builder()
                    .apiGroups("monitoring.coreos.com")
                    .resources("servicemonitors")
                    .verbs(                    
                        "get",
                        "create")
                    .build(),
                PolicyRule.builder()
                    .apiGroups("apps")
                    .resourceNames("pulumi-kubernetes-operator")
                    .resources("deployments/finalizers")
                    .verbs("update")
                    .build(),
                PolicyRule.builder()
                    .apiGroups("")
                    .resources("pods")
                    .verbs("get")
                    .build(),
                PolicyRule.builder()
                    .apiGroups("apps")
                    .resources(                    
                        "replicasets",
                        "deployments")
                    .verbs("get")
                    .build(),
                PolicyRule.builder()
                    .apiGroups("pulumi.com")
                    .resources("*")
                    .verbs(                    
                        "create",
                        "delete",
                        "get",
                        "list",
                        "patch",
                        "update",
                        "watch")
                    .build())
            .build());

        var pulumi_kubernetes_operatorRoleBinding = new RoleBinding("pulumi_kubernetes_operatorRoleBinding", RoleBindingArgs.builder()        
            .kind("RoleBinding")
            .apiVersion("rbac.authorization.k8s.io/v1")
            .metadata(ObjectMeta.builder()
                .name("pulumi-kubernetes-operator")
                .build())
            .subjects(Subject.builder()
                .kind("ServiceAccount")
                .name("pulumi-kubernetes-operator")
                .build())
            .roleRef(RoleRef.builder()
                .kind("Role")
                .name("pulumi-kubernetes-operator")
                .apiGroup("rbac.authorization.k8s.io")
                .build())
            .build());

        var pulumi_kubernetes_operatorServiceAccount = new ServiceAccount("pulumi_kubernetes_operatorServiceAccount", ServiceAccountArgs.builder()        
            .apiVersion("v1")
            .kind("ServiceAccount")
            .metadata(ObjectMeta.builder()
                .name("pulumi-kubernetes-operator")
                .build())
            .build());

        return ctx.exports();
    }
}
