// Copyright 2022, Pulumi Corporation.  All rights reserved.

package java

import (
	tmplt "text/template"
)

const (
	// MissingKeyErrorOption is the renderer option to stop execution immediately with an error on missing key
	MissingKeyErrorOption = "missingkey=error"
)

func Template(name string, text string) *tmplt.Template {
	return tmplt.Must(tmplt.New(name).Funcs(predefinedFunctions).Option(MissingKeyErrorOption).Parse(text))
}

var predefinedFunctions = tmplt.FuncMap{
	"eq": func(x, y interface{}) bool {
		return x == y
	},
	"sub": func(y, x int) int {
		return x - y
	},
}

// nolint:lll
const javaUtilitiesTemplateText = `

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import com.pulumi.core.internal.Environment;
import com.pulumi.deployment.InvokeOptions;
{{ .AdditionalImports }}
public class {{ .ClassName }} {

	public static Optional<java.lang.String> getEnv(java.lang.String... names) {
        for (var n : names) {
            var value = Environment.getEnvironmentVariable(n);
            if (value.isValue()) {
                return Optional.of(value.value());
            }
        }
        return Optional.empty();
    }

	public static Optional<java.lang.Boolean> getEnvBoolean(java.lang.String... names) {
        for (var n : names) {
            var value = Environment.getBooleanEnvironmentVariable(n);
            if (value.isValue()) {
                return Optional.of(value.value());
            }
        }
        return Optional.empty();
	}

	public static Optional<java.lang.Integer> getEnvInteger(java.lang.String... names) {
        for (var n : names) {
            var value = Environment.getIntegerEnvironmentVariable(n);
            if (value.isValue()) {
                return Optional.of(value.value());
            }
        }
        return Optional.empty();
	}

	public static Optional<java.lang.Double> getEnvDouble(java.lang.String... names) {
        for (var n : names) {
            var value = Environment.getDoubleEnvironmentVariable(n);
            if (value.isValue()) {
                return Optional.of(value.value());
            }
        }
        return Optional.empty();
	}
` + /* TODO: InvokeOptions probably should be done via a mutator on the InvokeOptions */ `
	public static InvokeOptions withVersion(@Nullable InvokeOptions options) {
            if (options != null && options.getVersion().isPresent()) {
                return options;
            }
            return new InvokeOptions(
                options == null ? null : options.getParent().orElse(null),
                options == null ? null : options.getProvider().orElse(null),
                getVersion()
            );
        }

    private static final java.lang.String version;
    public static java.lang.String getVersion() {
        return version;
    }

    static {
        var resourceName = "{{ .VersionPath }}/version.txt";
        var versionFile = Utilities.class.getClassLoader().getResourceAsStream(resourceName);
        if (versionFile == null) {
            throw new IllegalStateException(
                    java.lang.String.format("expected resource '%s' on Classpath, not found", resourceName)
            );
        }
        version = new BufferedReader(new InputStreamReader(versionFile))
                .lines()
                .collect(Collectors.joining("\n"))
                .trim();
    }{{ .PackageReferenceUtilities }}
}
`

var javaUtilitiesTemplate = Template("JavaUtilities", javaUtilitiesTemplateText)

type javaUtilitiesTemplateContext struct {
	VersionPath               string
	ClassName                 string
	Tool                      string
	AdditionalImports         string
	PackageReferenceUtilities string
}

const getterTemplateText = `{{ .Indent }}public {{ .GetterType }} {{ .GetterName }}() {
{{ .Indent }}    return {{ .ReturnStatement }};
{{ .Indent }}}`

var getterTemplate = Template("Getter", getterTemplateText)

type getterTemplateContext struct {
	Indent          string
	GetterType      string
	GetterName      string
	ReturnStatement string
}

type builderSetterTemplateContext struct {
	SetterName   string
	PropertyType string
	PropertyName string
	Assignment   string
	IsRequired   bool
	ListType     string
	Annotations  []string
}

type builderFieldTemplateContext struct {
	FieldType      string
	FieldName      string
	Initialization string
}

const builderTemplateText = `{{ .Indent }}public static {{ .Name }} builder() {
{{ .Indent }}    return new {{ .Name }}();
{{ .Indent }}}

{{ .Indent }}public static {{ .Name }} builder({{ .ResultType }} defaults) {
{{ .Indent }}    return new {{ .Name }}(defaults);
{{ .Indent }}}

{{- range $annotation := .Annotations }}
{{ $.Indent }}{{ $annotation }}
{{- end }}
{{ .Indent }}public static {{ if .IsFinal }}final {{ end }}class {{ .Name }} {
{{- range $field := .Fields }}
{{ $.Indent }}    private {{ $field.FieldType }} {{ $field.FieldName }}{{ $field.Initialization }};
{{- end }}
{{ $.Indent }}    public {{ .Name }}() {}
{{ $.Indent }}    public {{ .Name }}({{ .ResultType }} defaults) {
{{ $.Indent }}	      {{ .Objects }}.requireNonNull(defaults);
{{- range $field := .Fields }}
{{ $.Indent }}	      this.{{ $field.FieldName }} = defaults.{{ $field.FieldName }};
{{- end }}
{{ $.Indent }}    }
{{ range $setter := .Setters }}
{{- range $annotation := $setter.Annotations }}
{{ $.Indent }}    {{ $annotation }}
{{- end }}
{{ $.Indent }}    public {{ $.Name }} {{ $setter.SetterName }}({{ $setter.PropertyType }} {{ $setter.PropertyName }}) {
{{ if $setter.IsRequired }}{{ $.Indent }}        if ({{ $setter.PropertyName }} == null) {
{{ $.Indent }}          throw new MissingRequiredPropertyException("{{ $.ResultType }}", "{{ $setter.PropertyName }}");
{{ $.Indent }}        }{{ end }}
{{ $.Indent }}        {{ $setter.Assignment }};
{{ $.Indent }}        return this;
{{ $.Indent }}    }
{{- if $setter.ListType }}
{{ $.Indent }}    public {{ $.Name }} {{ $setter.SetterName }}({{ $setter.ListType }}... {{ $setter.PropertyName }}) {
{{ $.Indent }}        return {{ $setter.SetterName }}(List.of({{ $setter.PropertyName }}));
{{ $.Indent }}    }
{{- end -}}
{{ end }}
{{ $.Indent }}    public {{ .ResultType }} build() {
{{ $.Indent }}        final var _resultValue = new {{ .ResultType }}();
{{- range $i, $field := .Fields }}
{{ $.Indent }}        _resultValue.{{ $field.FieldName }} = {{ $field.FieldName }};
{{- end }}
{{ $.Indent }}        return _resultValue;
{{ .Indent }}    }
{{ .Indent }}}`

var builderTemplate = Template("Builder", builderTemplateText)

type builderTemplateContext struct {
	Indent      string
	Name        string
	IsFinal     bool
	Fields      []builderFieldTemplateContext
	Setters     []builderSetterTemplateContext
	ResultType  string
	Objects     string
	Annotations []string
}
