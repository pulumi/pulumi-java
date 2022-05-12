package main

import (
	"bytes"
	"encoding/json"
	"flag"
	"fmt"
	"io"
	"io/fs"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"text/template"

	"github.com/blang/semver"
	"github.com/pkg/errors"
	"gopkg.in/yaml.v3"

	javagen "github.com/pulumi/pulumi-java/pkg/codegen/java"
	pschema "github.com/pulumi/pulumi/pkg/v3/codegen/schema"
	"github.com/pulumi/pulumi/sdk/v3/go/common/resource/plugin"
)

type Config struct {
	ArtifactID    string      `yaml:"artifactID"`
	Version       string      `yaml:"version"`
	Schema        string      `yaml:"schema"`
	Out           string      `yaml:"out"`
	VersionFile   string      `yaml:"versionFile"`
	PackageInfo   interface{} `yaml:"packageInfo"`
	PluginFile    string      `yaml:"pluginFile"`
	Overlays      []string    `yaml:"overlays"`
	VersionEnvVar string      `yaml:"versionEnvVar"`
}

func main() {
	config := flag.String("config", "", "path to a YAML-encoded Config file; "+
		"if this option is set others take no effect")

	version := flag.String("version", "", "semantic version for the package")

	schema := flag.String("schema", "", "URL or local path to a schema, see "+
		"https://www.pulumi.com/docs/guides/pulumi-packages/schema/")

	override := flag.String("override", "", "path to a file with overrides for .language.java, see "+
		"https://www.pulumi.com/docs/guides/pulumi-packages/schema/#language-specific-extensions")

	out := flag.String("out", "", "output path")

	flag.Parse()

	var rootDir string
	var cfg *Config

	if config != nil && *config != "" {
		var err error
		rootDir, err = filepath.Abs(filepath.Dir(*config))
		if err != nil {
			log.Fatal(err)
		}

		cfg, err = parseConfig(*config)
		if err != nil {
			log.Fatal(err)
		}
	} else {
		var err error
		rootDir, err = os.Getwd()
		if err != nil {
			log.Fatal(err)
		}

		cfg = &Config{
			Schema:  *schema,
			Out:     *out,
			Version: *version,
		}

		if override != nil && *override != "" {
			pkgInfo, err := parsePackageInfoOverride(*override)
			if err != nil {
				log.Fatal(err)
			}
			cfg.PackageInfo = pkgInfo
		}
	}

	if err := generateJava(rootDir, *cfg); err != nil {
		log.Fatal(err)
	}
}

func parseConfig(path string) (*Config, error) {
	bytes, err := ioutil.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("Failed to read yaml config from %s: %w", path, err)
	}
	var cfg Config
	if err := yaml.Unmarshal(bytes, &cfg); err != nil {
		return nil, fmt.Errorf("Failed to parse yaml config from %s: %w", path, err)
	}
	if cfg.Schema == "" {
		return nil, fmt.Errorf("Missing required field in config at %s: schema", path)
	}
	if cfg.Out == "" {
		cfg.Out = "sdk/java"
	}
	if cfg.Version == "" {
		return nil, fmt.Errorf("Missing required field in config at %s: version", path)
	}
	if cfg.VersionEnvVar == "" {
		cfg.VersionEnvVar = fmt.Sprintf("PULUMI_%s_PROVIDER_SDK_VERSION",
			strings.ReplaceAll(strings.ToUpper(cfg.ArtifactID),
				"-", "_"))
	}
	return &cfg, nil
}

func parsePackageInfoOverride(path string) (interface{}, error) {
	bytes, err := ioutil.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("Failed to read language override from %s: %w", path, err)
	}
	var override interface{}
	if err := json.Unmarshal(bytes, &override); err != nil {
		return nil, fmt.Errorf("Failed to parse language override from %s: %w", path, err)
	}
	return &override, nil
}

func readPackageSchema(path string) (*pschema.PackageSpec, error) {
	var stream io.ReadCloser
	if strings.HasPrefix(path, "http") {
		resp, err := http.Get(path) // #nosec G107
		if err != nil {
			return nil, err
		}
		stream = resp.Body
	} else {
		file, err := os.Open(path)
		if err != nil {
			return nil, err
		}
		stream = file
	}

	defer stream.Close()
	var result pschema.PackageSpec
	if strings.HasSuffix(path, ".yaml") {
		dec := yaml.NewDecoder(stream)
		if err := dec.Decode(&result); err != nil {
			return nil, errors.Wrap(err, "reading YAML schema")
		}
	} else {
		dec := json.NewDecoder(stream)
		if err := dec.Decode(&result); err != nil {
			return nil, errors.Wrap(err, "reading JSON schema")
		}
	}
	return &result, nil
}

func convertPackageInfo(mapParsedFromYaml interface{}) (javagen.PackageInfo, error) {
	packageInfoJSON, err := json.Marshal(mapParsedFromYaml)
	if err != nil {
		return javagen.PackageInfo{}, err
	}

	var result javagen.PackageInfo
	if err := json.Unmarshal(packageInfoJSON, &result); err != nil {
		return javagen.PackageInfo{}, err
	}
	return result, nil
}

func generateJava(rootDir string, cfg Config) error {
	rawPkgSpec, err := readPackageSchema(cfg.Schema)
	if err != nil {
		return fmt.Errorf("failed to read schema from %s: %w", cfg.Schema, err)
	}

	pkgSpec, err := dedupTypes(rawPkgSpec)
	if err != nil {
		return fmt.Errorf("failed to dedup types in schema from %s: %w", cfg.Schema, err)
	}

	pkg, err := pschema.ImportSpec(*pkgSpec, nil)
	if err != nil {
		return fmt.Errorf("failed to import Pulumi schema: %w", err)
	}

	version := semver.MustParse(cfg.Version)
	pkg.Version = &version

	pkgInfo, err := convertPackageInfo(cfg.PackageInfo)
	if err != nil {
		return err
	}

	pkg.Language["java"] = pkgInfo

	extraFiles, err := readOverlays(rootDir, cfg)
	if err != nil {
		return err
	}
	files, err := javagen.GeneratePackage("pulumi-java-gen", pkg, extraFiles)
	if err != nil {
		return err
	}

	outDir := filepath.Join(rootDir, cfg.Out)

	if err := cleanDir(outDir); err != nil {
		return err
	}

	for f, bytes := range files {
		if err := emitFile(filepath.Join(outDir, f), bytes); err != nil {
			return err
		}
	}

	if cfg.VersionFile == "" {
		parts := strings.Split(pkgInfo.BasePackageOrDefault(), ".")
		cfg.VersionFile = filepath.Join(append(
			[]string{"src", "main", "resources"},
			append(parts, pkg.Name, "version.txt")...)...)
	}

	{
		f := filepath.Join(outDir, cfg.VersionFile)
		bytes := []byte(cfg.Version)
		if err := emitFile(f, bytes); err != nil {
			return fmt.Errorf("failed to generate version file at %s: %w", f, err)
		}
	}

	if cfg.PluginFile == "" {
		parts := strings.Split(pkgInfo.BasePackageOrDefault(), ".")
		cfg.PluginFile = filepath.Join(append(
			[]string{"src", "main", "resources"},
			append(parts, pkg.Name, "plugin.json")...)...)
	}

	{
		pulumiPlugin := &plugin.PulumiPluginJSON{
			Resource: true,
			Name:     pkg.Name,
			Version:  cfg.Version,
			Server:   pkg.PluginDownloadURL,
		}

		f := filepath.Join(outDir, cfg.PluginFile)
		bytes, err := (pulumiPlugin).JSON()
		if err != nil {
			return fmt.Errorf("failed to serialize plugin file at %s: %w", f, err)
		}
		if err := emitFile(f, bytes); err != nil {
			return fmt.Errorf("failed to generate plugin file at %s: %w", f, err)
		}
	}

	return nil
}

func cleanDir(path string) error {
	return os.RemoveAll(path)
}

func emitFile(path string, bytes []byte) error {
	if err := os.MkdirAll(filepath.Dir(path), os.ModePerm); err != nil {
		return fmt.Errorf("os.MkdirAll failed: %w", err)
	}
	if err := ioutil.WriteFile(path, bytes, 0600); err != nil {
		return fmt.Errorf("ioutil.WriteFile failed: %w", err)
	}
	return nil
}

const templateComment = `*** WARNING: this file was generated from an overlay template %s, do not edit. ***`

func readOverlays(rootDir string, cfg Config) (map[string][]byte, error) {
	result := map[string][]byte{}
	for _, overlay := range cfg.Overlays {
		overlayDir := filepath.Join(rootDir, overlay)
		err := filepath.WalkDir(overlayDir, func(path string, entry fs.DirEntry, err error) error {
			if err != nil {
				return err
			}
			if !entry.IsDir() {
				sourcePath := filepath.Join(overlayDir, entry.Name())
				bytes, err := ioutil.ReadFile(sourcePath)
				if err != nil {
					return err
				}
				if isOverlayTemplate(bytes) {
					data := OverlayTemplateData{
						ArtifactID:      cfg.ArtifactID,
						DefaultVersion:  cfg.Version,
						TemplateComment: fmt.Sprintf(templateComment, entry.Name()),
						VersionEnvVar:   cfg.VersionEnvVar,
					}
					bytes, err = expandOverlayTemplate(data, bytes)
					if err != nil {
						return err
					}
				}
				result[entry.Name()] = bytes
			}
			return err
		})
		if err != nil {
			return nil, err
		}
	}
	return result, nil
}

func isOverlayTemplate(bytes []byte) bool {
	return strings.Contains(string(bytes), ".ArtifactID")
}

type OverlayTemplateData struct {
	ArtifactID      string
	DefaultVersion  string
	TemplateComment string
	VersionEnvVar   string
}

func expandOverlayTemplate(
	data OverlayTemplateData,
	templateBytes []byte,
) ([]byte, error) {
	tmpl, err := template.New("OverlayTemplate").Parse(string(templateBytes))
	if err != nil {
		return nil, err
	}
	var out bytes.Buffer
	if err := tmpl.Execute(&out, data); err != nil {
		return nil, err
	}
	return out.Bytes(), nil
}
