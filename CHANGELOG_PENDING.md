### Improvements

- Plugin: clean up resources and exit cleanly on receiving SIGINT or CTRL_BREAK.
- Adds `MissingRequiredPropertyException` to the main java SDK to be used later in the generated provider SDKs.
- Plugin: will now automatically use the Gradle executor if build.gradle.kts is present

### Bug Fixes