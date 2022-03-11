# Pulumi Templates

Java templates are incubating here before being merged into the
[templates](https://github.com/pulumi/templates) repo.


## Adding a new template

1. Create a new directory for the template, e.g. `my-template-java`.
   By convention, hyphens are used to separate words and the language
   is included as a suffix.

2. Add template files in the new directory.


## Text replacement

The following special strings can be included in any template file;
these will be replaced by the CLI when laying down the template files.

 - `${PROJECT}` - The name of the project.
 - `${DESCRIPTION}` - The description of the project.


## Using a template

Here is how you can use templates from a local checkout of this
repository before they make it to the official template set.

Checkout this repo to `~/pulumi-java`.

Start a new empty Java Gradle Pulumi project called `p`:

```
pulumi new ~/pulumi-java/templates/java-gradle --dir p -n p --yes
```

Deploy the project:

```
cd p
pulumi up --yes
```

Dependencies:

- pulumi-language-jvm in PATH

- Pulumi SDK resolvable from local maven repo (~/.m2)


Installing dependencies:

```
cd ~/pulumi-java
make install_sdk
make bin/pulumi-language-jvm
export PATH=$PATH:$PWD/bin
```
