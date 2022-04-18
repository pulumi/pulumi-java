# Packages

Java binaries for the Pulumi Java SDK and resource providers use
[GitHub Packages Registry](https://github.com/features/packages)
(GPR) for distribution.

## Consuming Artifacts

### Auth

While `pulumi/pulumi-java` repository remains private, package
managers need to be authorized to access the Java libraries.

You will need:

- a GitHub username
- access grant to read `pulumi/pulumi-java` repository
- a [Personal Access Token](https://github.com/settings/tokens) with `read:package` scope

### Maven

TODO

### Gradle

Set `GPR_USER` and `GPR_TOKEN` environment variables to your GitHub
username and Personal Access Token respectively.

Add this to `build.gradle`:

```
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/pulumi/pulumi-java")
        credentials {
            username = project.findProperty("gpr.user")  ?: System.getenv("GPR_USER")
            password = project.findProperty("gpr.token") ?: System.getenv("GPR_TOKEN")
        }
    }
}

```

You can now reference dependencies such as:

```
dependencies {
    implementation 'com.pulumi:pulumi:0.0.1'
}
```

## Publishing Artifacts

We intend to setup automatic publishing as a GitHub action reacting to
a Git tag event.


## References

- [Working with a Github Packags Registry: Maven](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)

- [Working with a GitHub Packages Registry: Gradle](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry)
