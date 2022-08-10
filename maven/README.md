# Maven build support files

Support for accessing packages from
[GitHub Packages Registry](https://github.com/features/packages).

If `settings.xml` is copied to `/.m2`, it sets up resolving artifacts
from `https://maven.pkg.github.com/pulumi/pulumi-java`.

While the repo remains private, this additionally needs username and
token environment variables set:

```
<username>${env.GPR_USER}</username>
<password>${env.GPR_TOKEN}</password>
```

See `doc/packages.md`.
