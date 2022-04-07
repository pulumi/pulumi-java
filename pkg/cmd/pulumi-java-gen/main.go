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

	"gopkg.in/yaml.v3"

	jvmgen "github.com/pulumi/pulumi-java/pkg/codegen/jvm"
	pschema "github.com/pulumi/pulumi/pkg/v3/codegen/schema"
	"github.com/pulumi/pulumi/sdk/v3/go/common/resource/plugin"
)

type Config struct {
	ArtifactID  string      `yaml:"artifactID"`
	Version     string      `yaml:"version"`
	Schema      string      `yaml:"schema"`
	Out         string      `yaml:"out"`
	VersionFile string      `yaml:"versionFile"`
	PackageInfo interface{} `yaml:"packageInfo"`
	PluginFile  string      `yaml:"pluginFile"`
	Overlays    []string    `yaml:"overlays"`
}

func main() {
	config := flag.String("config", "pulumi-java-gen.yaml", "path to pulumi-java-gen.yaml")
	flag.Parse()

	if err := generateJava(*config); err != nil {
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
	return &cfg, nil
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
		jsonFile, err := os.Open(path)
		if err != nil {
			return nil, err
		}
		stream = jsonFile
	}
	defer stream.Close()
	dec := json.NewDecoder(stream)
	var result pschema.PackageSpec
	if err := dec.Decode(&result); err != nil {
		return nil, err
	}
	return &result, nil
}

func convertPackageInfo(mapParsedFromYaml interface{}) (jvmgen.PackageInfo, error) {
	packageInfoJSON, err := json.Marshal(mapParsedFromYaml)
	if err != nil {
		return jvmgen.PackageInfo{}, err
	}

	var result jvmgen.PackageInfo
	if err := json.Unmarshal(packageInfoJSON, &result); err != nil {
		return jvmgen.PackageInfo{}, err
	}
	return result, nil
}

func generateJava(configFile string) error {
	rootDir, err := filepath.Abs(filepath.Dir(configFile))
	if err != nil {
		return err
	}

	cfg, err := parseConfig(configFile)
	if err != nil {
		return err
	}

	pkgSpec, err := readPackageSchema(cfg.Schema)
	if err != nil {
		return fmt.Errorf("failed to read schema from %s: %w", cfg.Schema, err)
	}

	pkg, err := pschema.ImportSpec(*pkgSpec, nil)
	if err != nil {
		return fmt.Errorf("failed to import Pulumi schema: %w", err)
	}

	pkgInfo, err := convertPackageInfo(cfg.PackageInfo)
	if err != nil {
		return err
	}

	pkg.Language["jvm"] = pkgInfo

	extraFiles, err := readOverlays(rootDir, *cfg)
	if err != nil {
		return err
	}
	files, err := jvmgen.GeneratePackage("pulumi-java-gen", pkg, extraFiles)
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

	if cfg.VersionFile != "" {
		f := filepath.Join(outDir, cfg.VersionFile)
		bytes := []byte(cfg.Version)
		if err := emitFile(f, bytes); err != nil {
			return fmt.Errorf("failed to generate version file at %s: %w", f, err)
		}
	}

	// TODO(t0yv0): Curious, should this be part of GeneratePackage rather than pulumi-java-gen?
	//              I think that everything under GeneratePackage will end up shipping as part of machinery
	//              the users will use to build Pulumi Packages (for Java).
	//              On the other hand, pulumi-java-gen is internal repository helper for us in here.
	//              I think the plugin.json is something quite official so perhaps we can move?
	// TODO(pprazak): Would that also include version.txt above? The files are used together.
	if cfg.PluginFile != "" {
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

	if pkgInfo.BuildFiles == "gradle" {
		if err := emitFile(filepath.Join(outDir, "gradle.properties"),
			[]byte(fmt.Sprintf("version=%s", cfg.Version))); err != nil {
			return fmt.Errorf("failed to generate gradle.properties: %w", err)
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
