# Java Code Generator

Converts Pulumi Package Schema (JSON) into typed Java SDK source files (resources, args, outputs, enums, build files).

## Key files

- `gen.go` — main orchestrator: schema → module contexts → Java class files
- `gen_program.go` — HCL/PCL program → executable Java `Main` class
- `gen_program_expressions.go` — expression-level code generation (operators, traversals, function calls)
- `gen_defaults.go` — default value generation for resource args
- `templates_gradle.go` — Gradle build file templates for generated SDKs
- `package_info.go` — Java-specific package metadata from schema `.language.java`
- `names/` — identifier normalization (camelCase, PascalCase), Java reserved word handling, import management

## Commands

- Run tests: `go test ./...` (from this directory)
- Update golden files: `PULUMI_ACCEPT=true go test ./...`
- From repo root: `mise exec -- make codegen_tests` / `mise exec -- make codegen_tests_update`

## Golden file testing

- Tests compare generated output against fixtures in `testdata/` and `../testing/test/testdata/`
- Schema fixtures are borrowed from the `pulumi/` submodule via `mise exec -- make borrow_schemas`
- When codegen changes produce new output, run `PULUMI_ACCEPT=true go test ./...` to update fixtures, then review the diff carefully
- Never hand-edit golden files

## Forbidden patterns

- Do not import `github.com/pulumi/pulumi/pkg/v3/` — only `codegen` and `testing` packages from pulumi/pulumi are allowed
- Do not add Java reserved words to generated identifiers without going through `names/ident.go`
