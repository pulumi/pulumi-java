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
