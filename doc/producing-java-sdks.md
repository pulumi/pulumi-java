# Producing Java SDKs

Java artifacts for the `pulumi/pulumi-java` core SDK and every resource provider are published to Maven Central. This
process is automated. This page is a reference for how publishing is set up.

## Playbooks

### Diagnosing publishing failure

Pulumi CI uses `io.github.gradle-nexus.publish-plugin` to publish to Maven Central. Originally the OSSRH repository was
used, but it has since been sunset by the Central maintainers. The current setup uses an
[OSSRH-style staging API](https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/) to minimize the
migration pain.

Publishing has proven unreliable at times, so CI jobs are configured to ignore failures during Java publishing to avoid
blocking the other languages.

Please feel free to edit this page with any new learnings on operating the publishing pipeline.

### Rotating the publishing token

- Sign in to the Maven Central portal using the shared Maven Central account (kept in the org secret store).

- Follow the [generate-token](https://central.sonatype.org/publish/generate-portal-token/) instructions to generate a
  new username and token pair.

- Save the new pair in the org secret store.

- Update the corresponding GitHub organization secrets for the `pulumi` organization with the newly generated
  username/token pair.

### Rotating the signing keys

The JAR artifacts are signed with a private GPG key, with the public half published to global key servers.

To rotate the key:

- Generate a new GPG key with a passphrase.
- Update the entries in the org secret store.
- Update the corresponding GitHub organization secrets.

## Reference

### Publishing credentials

Publishing to Maven Central requires two credentials, both kept in the org secret store:

- **Maven Central publishing token** — the username/token pair CI uses to publish. It is generated (or reset) from the
  Maven Central web portal (https://central.sonatype.com/account) using the shared Maven Central account; see the
  [generate-portal-token](https://central.sonatype.org/publish/generate-portal-token/) instructions. The legacy OSSRH
  publishing token is obsolete now that [OSSRH has been sunset](https://central.sonatype.org/pages/ossrh-eol/#self-service-migration).

- **Artifact signing key** — a private GPG key, plus its passphrase, used by Gradle to sign the artifacts.

### GitHub organization secrets

The publishing and signing credentials above are injected into Pulumi CI/CD jobs through GitHub organization secrets:
the Maven Central username/token, and the GPG signing key content, key ID, and passphrase.

### ESC environments

Pulumi CI jobs are gradually migrating to use ESC. The relevant ESC environment may need updating when the keys are
rotated or something else changes.
