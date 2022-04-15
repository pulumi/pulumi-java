# Maven build support files

Support for accessing packages from
[GitHub Packages Registry](https://github.com/features/packages).

If `settings.xml` is copied to `/.m2`, it sets up resolving artifacts
from `https://maven.pkg.github.com/pulumi/pulumi-java`.

While the repo remains private, this additionally needs username and
token env vars set:

```
<username>${env.GPR_USER}</username>
<password>${env.GPR_TOKEN}</password>
```

See `doc/packages.md`.

The `dummy` project is included to help prefetching correct versioned
provider jars into a local maven repo for testing.
