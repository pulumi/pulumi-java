**Pulumi Java SDK** lets you leverage the full power of [Pulumi Infrastructure as Code Platform](https://pulumi.com) using the Java programming language.


Simply write Java code in your favorite editor and Pulumi
automatically provisions and manages your AWS, Azure, Google Cloud
Platform, and/or Kubernetes resources, using an infrastructure-as-code
approach. Use standard language features like loops, functions,
classes, and IDE features like refactorig and package management that
you already know and love.

For example, create three web servers:

```java
package myinfra;

import com.pulumi.Pulumi;
import com.pulumi.aws.ec2.Instance;
import com.pulumi.aws.ec2.InstanceArgs;
import com.pulumi.aws.ec2.SecurityGroup;
import com.pulumi.aws.ec2.SecurityGroupArgs;
import com.pulumi.aws.ec2.enums.InstanceType;
import com.pulumi.aws.ec2.inputs.SecurityGroupIngressArgs;

import java.util.List;

public final class Infra {
    public static void main(String[] args) {
        Pulumi.run(ctx -> {
            final var sg = new SecurityGroup("web-sg", SecurityGroupArgs.builder()
                    .ingress(SecurityGroupIngressArgs.builder()
                            .protocol("tcp")
                            .fromPort(80)
                            .toPort(80)
                            .cidrBlocks("0.0.0.0/0")
                            .build())
                    .build());
            for (var i = 0; i < 3; i++) {
                new Instance(String.format("web-%d", i), InstanceArgs.builder()
                        .ami("ami-7172b611")
                        .instanceType(InstanceType.T2_Micro)
                        .securityGroups(sg.name().applyValue(List::of))
                        .userData(String.join("\n",
                                "#!/bin/bash",
                                "echo \"Hello, World!\" > index.html",
                                "nohup python -m SimpleHTTPServer 80 &"))
                        .build());
            }
        });
    }
}
```


## Welcome

* **[Get Started with Pulumi using Java](#getting-started)**: Deploy a simple application in AWS, Azure, Google Cloud or Kubernetes using Pulumi to describe the desired infrastructure using Java.

* **[Examples](https://github.com/pulumi/examples)**: Browse Java examples across many clouds and scenarios including containers, serverless, and infrastructure.

* **[Docs](https://www.pulumi.com/docs/)**: Learn about Pulumi concepts, follow user-guides, and consult the reference documentation. Java examples are included.

* **[Community Slack](https://slack.pulumi.com/?utm_campaign=pulumi-pulumi-github-repo&utm_source=github.com&utm_medium=welcome-slack)**: Join us in Pulumi Community Slack. All conversations and questions are welcome.

* **[GitHub Discussions](https://github.com/pulumi/pulumi/discussions)**: Ask questions or share what you are building with Pulumi.


## <a name="getting-started"></a>Getting Started

The following steps demonstrate how to declare your first cloud
resources in a Pulumi Java, and deploy them to AWS, in minutes:

1. **Install Pulumi**:

    To install the latest Pulumi release, run the following (see full
    [installation instructions](https://www.pulumi.com/docs/reference/install/?utm_campaign=pulumi-pulumi-github-repo&utm_source=github.com&utm_medium=getting-started-install) for additional installation options):

    ```bash
    $ curl -fsSL https://get.pulumi.com/ | sh
    ```

2. **Create a Project**:

    After installing, you can get started with the `pulumi new` command:

    ```bash
    $ mkdir pulumi-java-demo && cd pulumi-java-demo
    $ pulumi new aws-java
    ```

    The `new` command offers templates, including Java templates, for
    all clouds. Run it without an argument and it'll prompt you with
    available projects.

3. **Deploy to the Cloud**:

    Run `pulumi up` to get your code to the cloud:

    ```bash
    $ pulumi up
    ```

    This makes all cloud resources declared in your code. Simply make
    edits to your project, and subsequent `pulumi up`s will compute
    the minimal diff to deploy your changes.

4. **Use Your Program**:

    Now that your code is deployed, you can interact with it. In the
    above example, we can find the name of the newly provisioned S3
    bucket:

    ```bash
    $ pulumi stack output bucketName
    ```

To learn more, head over to [pulumi.com](https://pulumi.com/?utm_campaign=pulumi-pulumi-github-repo&utm_source=github.com&utm_medium=getting-started-learn-more-home) for much more information, including
[tutorials](https://www.pulumi.com/docs/reference/tutorials/?utm_campaign=pulumi-pulumi-github-repo&utm_source=github.com&utm_medium=getting-started-learn-more-tutorials), [examples](https://github.com/pulumi/examples), and
details of the core Pulumi CLI and [programming model concepts](https://www.pulumi.com/docs/reference/concepts/?utm_campaign=pulumi-pulumi-github-repo&utm_source=github.com&utm_medium=getting-started-learn-more-concepts).


## Requirements

JDK 11 or higher is required.

Apache Maven is the recommended build tool. Gradle Build Tool is also
supported. Pulumi will recognize Maven and Gradle programs and
automatically recompile them without any further configuration. The
supported versions are:

- Apache Maven 3.8.4
- Gradle Build Tool 7.4

Other build tools are supported via the `runtime.options.binary`
configuration option that can point to a pre-built jar in
`Pulumi.yaml`:

```
name: myproject
runtime:
  name: java
  options:
    binary: target/myproject-1.0-SNAPSHOT-jar-with-dependencies.jar
```



## Contributing

Visit
[CONTRIBUTING.md](https://github.com/pulumi/pulumi-java/blob/master/CONTRIBUTING.md)
for information on building Pulumi Java support from source or
contributing improvements.
