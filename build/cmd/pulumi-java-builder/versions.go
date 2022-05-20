package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/url"
)

type versionId string

func findVersionId(token, pkg, version string) (versionId, error) {
	versionJson, err := jsonEncode(pkg)
	if err != nil {
		return "", err
	}

	var query string = fmt.Sprintf(`
          query {
            repository(owner: "pulumi", name: "pulumi-java") {
              packages(first: 1, names: [%s]) {
                edges {
                  node {
                    name
                    versions(last: 100) {
                      edges {
                        node {
                          id
                          version
                        }
                      }
                    }
                  }
                }
              }
            }
          }`, versionJson)
	out, err := graphql(token, query)
	if err != nil {
		return "", err
	}
	out = dot(out, "data", "repository", "packages", "edges")
	out = unlist(out)
	out = dot(out, "node", "versions", "edges")

	for _, entry := range out.([]interface{}) {
		v := dot(entry, "node", "version").(string)
		id := dot(entry, "node", "id").(string)
		if v == version {
			return versionId(id), nil
		}
	}

	return "", fmt.Errorf("Package version %s not found", version)
}

func deleteVersion(token string, versionId versionId) error {
	versionJson, err := jsonEncode(string(versionId))
	if err != nil {
		return err
	}
	var query string = fmt.Sprintf(`
            mutation {
              deletePackageVersion(input: {
                packageVersionId:%s
              }) {
                success
              }
            }
        `, versionJson)
	result, err := graphql(token, query)
	if err != nil {
		return err
	}

	if success, ok := dot(result, "data", "deletePackageVersion", "success").(bool); ok && success {
		return nil
	}

	var buf bytes.Buffer
	enc := json.NewEncoder(&buf)
	enc.SetIndent("", "  ")
	enc.Encode(result)
	return fmt.Errorf("Could not delete package version: %s", buf.String())
}

func deletePackageVersion(token, pkg, version string) error {
	vid, err := findVersionId(token, pkg, version)
	if err != nil {
		return err
	}
	if err := deleteVersion(token, vid); err != nil {
		return err
	}
	return nil
}

func jsonEncode(x string) (string, error) {
	var buf bytes.Buffer
	err := json.NewEncoder(&buf).Encode(x)
	if err != nil {
		return "", err
	}
	return buf.String(), nil
}

func dot(data interface{}, path ...string) interface{} {
	for _, p := range path {
		if m, ok := data.(map[string]interface{}); ok {
			if v, found := m[p]; found {
				data = v
			}
		}
	}
	return data
}

func unlist(data interface{}) interface{} {
	if list, ok := data.([]interface{}); ok && len(list) == 1 {
		return list[0]
	}
	return data
}

func graphql(token, query string) (interface{}, error) {
	var buf bytes.Buffer
	err := json.NewEncoder(&buf).Encode(map[string]string{
		"query": query,
	})
	if err != nil {
		return nil, err
	}
	url, err := url.Parse("https://api.github.com/graphql")
	if err != nil {
		return nil, err
	}
	var req = &http.Request{
		Method: "POST",
		URL:    url,
		Body:   ioutil.NopCloser(&buf),
		Header: http.Header{},
	}
	req.Header.Add("Content-Type", "application/json")
	req.Header.Add("Accept", "application/vnd.github.package-deletes-preview+json")
	req.Header.Add("Authorization", fmt.Sprintf("bearer %s", token))
	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return nil, err
	}
	if resp.StatusCode != 200 {
		return nil, fmt.Errorf("HTTP request failed: status %d",
			resp.StatusCode)
	}
	var out interface{}
	err = json.NewDecoder(resp.Body).Decode(&out)
	return out, err
}
