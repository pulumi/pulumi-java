### Improvements

- Update to Pulumi 3.144.1

- Implement `GetRequiredPackages` for the Java language host

- Support exporting plain stack output values with `Context.export`

- Support `StackReference.getOutput` from generated programs

- Implement `getOrganizationName` on `Deployment` and `Context`

### Bug Fixes

- [Convert] Emit the `Deployment` class when using Pulumi built-in functions in PCL `stack()` and `projectName()`