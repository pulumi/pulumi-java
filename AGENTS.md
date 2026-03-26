# Agent Instructions

## What this repo is

Pulumi Java language support: a Go-based language plugin (gRPC server), a Java code generator, and the Java SDK runtime library (`com.pulumi:pulumi`). The plugin lets the Pulumi CLI run Java programs; the codegen turns Pulumi Package schemas into typed Java SDKs.

## Repo structure

- `pkg/` — Go code: language plugin, code generator, executor abstraction
  - `pkg/cmd/pulumi-language-java/` — gRPC language runtime server
  - `pkg/cmd/pulumi-java-gen/` — code generator CLI (deprecated, use `pulumi package gen-sdk`)
  - `pkg/codegen/java/` — schema-to-Java codegen engine
  - `pkg/internal/executors/` — build tool abstraction (Gradle, Maven, JBang, SBT, JAR)
- `sdk/java/` — Gradle project for the Java SDK runtime
- `tests/` — integration tests, examples, templates
- `pulumi/` — git submodule of `pulumi/pulumi` (protobuf definitions, test schemas)
- `scripts/` — release, tidy, schema borrowing utilities

## Tool setup

This repo uses [mise](https://mise.jdx.dev/) to manage tool versions (Go, JDK, Gradle, Maven, etc.). See `.mise.toml` for the full list. If mise is installed and activated (via `mise activate` in your shell profile), tool versions are handled automatically and you can run `make` directly. Otherwise, **prefix all `make` commands with `mise exec --`** to ensure the correct tool versions are used:

```sh
mise exec -- make build
mise exec -- make lint
```

Protobuf definitions come from the `pulumi/` git submodule, not vendored copies.

## Command canon

All commands assume you're at the repo root.

- **Build all:** `mise exec -- make build`
- **Build Go binaries only:** `mise exec -- make build_go`
- **Build Java SDK only:** `mise exec -- make build_sdk`
- **Install SDK to ~/.m2:** `mise exec -- make install_sdk`
- **Install Go binaries to GOPATH:** `mise exec -- make install_go`
- **Lint:** `mise exec -- make lint`
- **Go tests:** `mise exec -- make test_go`
- **Codegen tests:** `mise exec -- make codegen_tests`
- **Update codegen golden files:** `mise exec -- make codegen_tests_update`
- **Integration tests:** `mise exec -- make test_integrations`
- **Single integration:** `mise exec -- make test_integration.<name>` (e.g. `make test_integration.stack-reference`)
- **Single example:** `mise exec -- make test_example.<name>` (e.g. `make test_example.minimal`)
- **Single template:** `mise exec -- make test_template.<name>` (e.g. `make test_template.java-gradle`)
- **Java SDK unit tests:** `cd sdk/java && mise exec -- gradle --console=plain build`
- **Java SDK all tests:** `cd sdk/java && mise exec -- gradle --console=plain build testAll`
- **Tidy Go modules:** `mise exec -- make tidy`
- **Borrow schemas from submodule:** `mise exec -- make borrow_schemas`
- **Changelog entry:** `mise exec -- make changelog` (interactive)

## Key invariants

- Go code lives under `pkg/`, not at repo root. Run `cd pkg && go test ./...` (not from root).
- `pkg/` Go code (outside `codegen/` and `testing/`) must not import `github.com/pulumi/pulumi/pkg/v3/` — enforced by `make lint`.
- Java toolchain targets JDK 11 (not 17) — see `sdk/java/pulumi/build.gradle`.
- SDK version comes from `PULUMI_JAVA_SDK_VERSION` env var (defaults to `0.0.1` locally).
- Golden file tests in `pkg/codegen/testing/test/testdata/` verify codegen output stability. Update with `PULUMI_ACCEPT=true`.
- Changelog entries are required for most PRs. Run `mise exec -- make changelog` to create one.
- Two things named "codegen": `pkg/codegen/java/` is the Go generator; `sdk/java/.../com/pulumi/codegen/` is Java runtime helpers used by generated code.

## Forbidden actions

- Do not run `git push --force`, `git reset --hard`, or `rm -rf` without explicit approval.
- Do not skip linting or use `--no-verify` on commits.
- Do not edit files under `pkg/codegen/testing/test/testdata/` by hand — update with `mise exec -- make codegen_tests_update`.
- Do not edit protobuf-generated Java files in `sdk/java/pulumi/build/generated/` — rebuilt by Gradle.
- Do not fabricate test output or changelog entries.
- Do not make sweeping changes, refactor unrelated code, or add unnecessary abstractions.
- Do not commit `build/` directories, `bin/` artifacts, or `.m2` cache files.

## Escalate immediately if

- A change affects the gRPC interface or protobuf definitions.
- Codegen golden file updates produce unexpectedly large diffs.
- Tests fail after two debugging attempts.
- A change touches the Maven Central publishing pipeline.
- Requirements are ambiguous about backwards compatibility.

## If you change...

- Any `.go` file in `pkg/` → `mise exec -- make lint && mise exec -- make test_go`
- `pkg/codegen/java/*.go` → `mise exec -- make lint && mise exec -- make codegen_tests` (update golden files if intentional: `mise exec -- make codegen_tests_update`)
- Java SDK source in `sdk/java/` → `cd sdk/java && mise exec -- gradle --console=plain build`
- `go.mod` or `go.sum` in any module → `mise exec -- make tidy`
- Test projects in `testdata/projects/` → `mise exec -- make test_go`
- Integration test code → `mise exec -- make test_integrations`

See `pkg/codegen/java/AGENTS.md` for codegen-specific instructions.
