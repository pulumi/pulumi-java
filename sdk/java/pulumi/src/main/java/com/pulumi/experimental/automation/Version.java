// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a semantic version (SemVer).
 */
public final class Version implements Comparable<Version> {
    // This class is a wrapper around a SemVer version so we don't have to directly
    // expose anything from the 3rd party SemVer library which isn't yet 1.0 stable.
    // This allows us to change the underlying implementation without breaking
    // changes in the future. For now, we only publicly expose the minimal amount of
    // functionality that's needed for basic version comparisons.

    private final com.github.zafarkhaja.semver.Version version;

    private Version(com.github.zafarkhaja.semver.Version version) {
        this.version = version;
    }

    /**
     * Obtains a {@link Version} instance of the specified major, minor and
     * patch versions.
     *
     * @param major a major version number, non-negative
     * @param minor a minor version number, non-negative
     * @return the version
     */
    public static Version of(long major, long minor) {
        return Version.of(major, minor, 0);
    }

    /**
     * Obtains a {@link Version} instance of the specified major, minor and
     * patch versions.
     *
     * @param major a major version number, non-negative
     * @param minor a minor version number, non-negative
     * @param patch a patch version number, non-negative
     * @return the version
     */
    public static Version of(long major, long minor, long patch) {
        return new Version(com.github.zafarkhaja.semver.Version.of(major, minor, patch));
    }

    /**
     * Parses a {@link Version} from the specified SemVer string.
     *
     * @param version the SemVer string to parse
     * @return the version
     */
    public static Version parse(String version) {
        var parsed = com.github.zafarkhaja.semver.Version.parse(version, false);
        return new Version(parsed);
    }

    /**
     * Parses a {@link Version} from the specified SemVer string.
     *
     * @param version the SemVer string to parse
     * @return the version if it can be parsed; empty otherwise
     */
    public static Optional<Version> tryParse(String version) {
        var parsed = com.github.zafarkhaja.semver.Version.tryParse(version, false);
        return parsed.isPresent()
                ? Optional.of(new Version(parsed.get()))
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
     * Determines if this {@link Version} has a higher precedence compared
     * with the specified {@link Version}.
     *
     * @param other the {@link Version} to compare with, non-null
     * @return {@code true}, if this {@link Version} is higher than the other
     *         {@link Version}; {@code false} otherwise
     */
    public boolean isHigherThan(Version other) {
        return version.isHigherThan(Objects.requireNonNull(other).version);
    }

    /**
     * Determines if this {@link Version} has a higher or equal precedence
     * compared with the specified {@link Version}.
     *
     * @param other the {@link Version} to compare with, non-null
     * @return {@code true}, if this {@link Version} is higher than or
     *         equivalent to the other {@link Version}; {@code false}
     *         otherwise
     */
    public boolean isHigherThanOrEquivalentTo(Version other) {
        return version.isHigherThanOrEquivalentTo(Objects.requireNonNull(other).version);
    }

    /**
     * Determines if this {@link Version} has a lower precedence compared with
     * the specified {@link Version}.
     *
     * @param other the {@link Version} to compare with, non-null
     * @return {@code true}, if this {@link Version} is lower than the other
     *         {@link Version}; {@code false} otherwise
     */
    public boolean isLowerThan(Version other) {
        return version.isLowerThan(Objects.requireNonNull(other).version);
    }

    /**
     * Determines if this {@link Version} has a lower or equal precedence
     * compared with the specified {@link Version}.
     *
     * @param other the {@link Version} to compare with, non-null
     * @return {@code true}, if this {@link Version} is lower than or
     *         equivalent to the other {@link Version}; {@code false}
     *         otherwise
     */
    public boolean isLowerThanOrEquivalentTo(Version other) {
        return version.isLowerThanOrEquivalentTo(Objects.requireNonNull(other).version);
    }

    /**
     * Determines if this {@link Version} has the same precedence as the
     * specified {@link Version}.
     *
     * @param other the {@link Version} to compare with, non-null
     * @return {@code true}, if this {@link Version} is equivalent to the
     *         other {@link Version}; {@code false} otherwise
     */
    public boolean isEquivalentTo(Version other) {
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
        var that = (Version) o;
        return Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version);
    }

    @Override
    public int compareTo(Version o) {
        return version.compareTo(o.version);
    }

    @Override
    public String toString() {
        return version.toString();
    }
}
