package main

import (
	"fmt"
	"log"
	"time"
)

func publish(token, dir, pkg string) error {
	autoPkg, version, err := readGradleProperties(dir)
	if err != nil {
		return err
	}
	if pkg == "" {
		pkg = autoPkg
	}

	backoff := 5 * time.Second

	var attempt int
	for attempt = 1; attempt <= 3; attempt++ {
		out, err := gradlePublish()
		if err == nil {
			if attempt > 1 {
				log.Printf("gradle publish suceeded (attempt %d)\n",
					attempt)
			}
			return nil
		}
		log.Printf("gradle publish failed (attempt %d): %s\n%s\n",
			attempt, err, out)
		log.Printf("Backing off for %v\n", backoff)
		time.Sleep(backoff)
		log.Printf("Clearing version %s\n", version)
		err = deletePackageVersion(token, pkg, version)
		if err != nil {
			log.Printf("Ignoring failure to delete version %s: %w",
				version, err)
		} else {
			log.Printf("Deleted version %s\n", version)
		}
	}
	return fmt.Errorf("Aborting after %d failed attempts", attempt)
}
