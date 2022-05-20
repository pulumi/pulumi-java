package main

import (
	"fmt"
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
	cmd.AddCommand(newPublishCommand())
	return cmd
}

func newPublishCommand() *cobra.Command {
	cmd := &cobra.Command{
		Use:   "publish",
		Short: "Like gradle-publish with retries and GitHub packages cleanup",
	}

	cmd.Run = func(cmd *cobra.Command, args []string) {
		token := os.Getenv("GITHUB_TOKEN")
		if token == "" {
			log.Fatalf("GITHUB_TOKEN env var is not set")
		}
		pkg, version, err := readGradleProperties()
		if err != nil {
			log.Fatal(err)
		}
		var attempt int
		for attempt = 1; attempt <= 3; attempt++ {
			out, err := gradlePublish()
			if err == nil {
				return
			}
			log.Printf("gradle publish failed, retrying (attempt %d): %s\n%s\n",
				attempt, err, out)
			if err := deletePackageVersion(token, pkg, version); err != nil {
				log.Fatal(err)
			}
		}
		log.Fatal(fmt.Errorf("Aborting after %d failed attempts", attempt))
	}

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
		if err := deletePackageVersion(token, pkg, version); err != nil {
			log.Fatal(err)
		}
	}
	return cmd
}
