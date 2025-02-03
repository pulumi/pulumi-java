// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.commands;

import java.util.Objects;
import java.util.Optional;

import com.github.zafarkhaja.semver.Version;
import com.pulumi.experimental.automation.ProjectRuntime;

/**
 * Represents a Pulumi version.
 */
public final class PulumiVersion implements Comparable<PulumiVersion> {
    // This class is a wrapper around a SemVer version so we don't have to directly
    // expose anything from the 3rd party SemVer library. This allows us to change
    // the underlying implementation without breaking changes in the future. For
    // now, we only expose the minimal amount of functionality that's needed.

    private final Version version;

    private PulumiVersion(Version version) {
        this.version = version;
    }

    /**
     * Obtains a {@link PulumiVersion} instance of the specified major, minor and
     * patch versions.
     *
     * @param major a major version number, non-negative
     * @param minor a minor version number, non-negative
     * @return the version
     */
    public static PulumiVersion of(long major, long minor) {
        return PulumiVersion.of(major, minor, 0);
    }

    /**
     * Obtains a {@link PulumiVersion} instance of the specified major, minor and
     * patch versions.
     *
     * @param major a major version number, non-negative
     * @param minor a minor version number, non-negative
     * @param patch a patch version number, non-negative
     * @return the version
     */
    public static PulumiVersion of(long major, long minor, long patch) {
        return new PulumiVersion(Version.of(major, minor, patch));
    }

    /**
     * Parses a {@link PulumiVersion} from the specified SemVer string.
     *
     * @param version the SemVer string to parse
     * @return the version
     */
    public static PulumiVersion parse(String version) {
        var parsed = Version.parse(version, false);
        return new PulumiVersion(parsed);
    }

    /**
     * Parses a {@link PulumiVersion} from the specified SemVer string.
     *
     * @param version the SemVer string to parse
     * @return the version if it can be parsed; empty otherwise
     */
    public static Optional<PulumiVersion> tryParse(String version) {
        var parsed = Version.tryParse(version, false);
        return parsed.isPresent()
                ? Optional.of(new PulumiVersion(parsed.get()))
                : Optional.empty();
    }

    /**
     * Returns the major version.
     *
     * @return the major version number
     */
    public long majorVersion() {
        return version.majorVersion();
    }

    /**
     * Returns the minor version.
     *
     * @return the minor version number
     */
    public long minorVersion() {
        return version.minorVersion();
    }

    /**
     * Returns the patch version.
     *
     * @return the patch version number
     */
    public long patchVersion() {
        return version.patchVersion();
    }

    /**
     * Determines if this {@link PulumiVersion} has a higher precedence compared
     * with
     * the specified {@link PulumiVersion}.
     *
     * @param other the {@link PulumiVersion} to compare with, non-null
     * @return {@code true}, if this {@link PulumiVersion} is higher than the other
     *         {@link PulumiVersion}; {@code false} otherwise
     */
    public boolean isHigherThan(PulumiVersion other) {
        return version.isHigherThan(Objects.requireNonNull(other).version);
    }

    /**
     * Determines if this {@link PulumiVersion} has a higher or equal precedence
     * compared with the specified {@link PulumiVersion}.
     *
     * @param other the {@link PulumiVersion} to compare with, non-null
     * @return {@code true}, if this {@link PulumiVersion} is higher than or
     *         equivalent to the other {@link PulumiVersion}; {@code false}
     *         otherwise
     */
    public boolean isHigherThanOrEquivalentTo(PulumiVersion other) {
        return version.isHigherThanOrEquivalentTo(Objects.requireNonNull(other).version);
    }

    /**
     * Determines if this {@link PulumiVersion} has a lower precedence compared with
     * the specified {@link PulumiVersion}.
     *
     * @param other the {@link PulumiVersion} to compare with, non-null
     * @return {@code true}, if this {@link PulumiVersion} is lower than the other
     *         {@link PulumiVersion}; {@code false} otherwise
     */
    public boolean isLowerThan(PulumiVersion other) {
        return version.isLowerThan(Objects.requireNonNull(other).version);
    }

    /**
     * Determines if this {@link PulumiVersion} has a lower or equal precedence
     * compared with the specified {@code PulumiVersion}.
     *
     * @param other the {@link PulumiVersion} to compare with, non-null
     * @return {@code true}, if this {@link PulumiVersion} is lower than or
     *         equivalent to the other {@link PulumiVersion}; {@code false}
     *         otherwise
     */
    public boolean isLowerThanOrEquivalentTo(PulumiVersion other) {
        return version.isLowerThanOrEquivalentTo(Objects.requireNonNull(other).version);
    }

    /**
     * Determines if this {@link PulumiVersion} has the same precedence as the
     * specified {@code PulumiVersion}.
     *
     * @param other the {@link PulumiVersion} to compare with, non-null
     * @return {@code true}, if this {@link PulumiVersion} is equivalent to the
     *         other {@link PulumiVersion}; {@code false} otherwise
     */
    public boolean isEquivalentTo(PulumiVersion other) {
        return version.isEquivalentTo(Objects.requireNonNull(other).version);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (PulumiVersion) o;
        return Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version);
    }

    @Override
    public int compareTo(PulumiVersion o) {
        return version.compareTo(o.version);
    }

    @Override
    public String toString() {
        return version.toString();
    }
}
