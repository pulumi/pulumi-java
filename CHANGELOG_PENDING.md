### Improvements

- Adds `MissingRequiredPropertyException` to the main java SDK to be used later in the generated provider SDKs.

### Bug Fixes

- Fixes `builder()` implementation for result types where the identifier of the local variable defined for the result type collides with one of the fields of the result type.
- Adds `options.encoding = "UTF-8"` to the `compileJava` options so that it can handle non-ASCII characters in the source code, especially in the documentation comments of fields.
- Fixes MockMonitor reporting DeletedWith wasn't supported
