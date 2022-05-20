package main

import (
	"log"
	"os"

	"github.com/spf13/cobra"
)

func main() {
	if err := newPulumiJavaBuilderCmd().Execute(); err != nil {
		log.Fatal(err)
	}
}

func newPulumiJavaBuilderCmd() *cobra.Command {
	cmd := &cobra.Command{
		Use:   "pulumi-java-builder",
		Short: "pulumi-java-builder: Build helpers",
	}
	cmd.AddCommand(newDeleteVersionCommand())
	return cmd
}

func newDeleteVersionCommand() *cobra.Command {
	cmd := &cobra.Command{
		Use:   "delete-version",
		Short: "Deletes a package version from GitHub packages",
	}
	var pkg, version string
	cmd.Flags().StringVar(&pkg, "pkg", "", "package to delete the version for")
	if err := cmd.MarkFlagRequired("pkg"); err != nil {
		log.Fatal(err)
	}
	cmd.Flags().StringVar(&version, "version", "", "version to delete")
	if err := cmd.MarkFlagRequired("version"); err != nil {
		log.Fatal(err)
	}
	cmd.Run = func(cmd *cobra.Command, args []string) {
		token := os.Getenv("GITHUB_TOKEN")
		if token == "" {
			log.Fatalf("GITHUB_TOKEN env var is not set")
		}
		vid, err := findVersionId(token, pkg, version)
		if err != nil {
			log.Fatal(err)
		}
		if err := deleteVersion(token, vid); err != nil {
			log.Fatal(err)
		}
	}
	return cmd
}
