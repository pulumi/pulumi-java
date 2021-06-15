package io.pulumi.core.internal;

import com.google.common.collect.ComparisonChain;
import io.pulumi.core.Tuples;
import io.pulumi.core.Tuples.Tuple5;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A semantic version implementation.
 * Conforms with v2.0.0 of http://semver.org
 */
public final class SemanticVersion implements Comparable<SemanticVersion> {

    private static final String ParseExpressionGroupMajor = "major";
    private static final String ParseExpressionGroupMinor = "minor";
    private static final String ParseExpressionGroupPatch = "patch";
    private static final String ParseExpressionGroupPre = "pre";
    private static final String ParseExpressionGroupBuild = "build";
    private static final Pattern ParseExpression = Pattern.compile("^(?<major>\\d+)(?>\\.(?<minor>\\d+))?(?>\\.(?<patch>\\d+))?(?>\\-(?<pre>[0-9A-Za-z\\-\\.]+))?(?>\\+(?<build>[0-9A-Za-z\\-\\.]+))?$");

    private final int major;
    private final int minor;
    private final int patch;
    private final String prerelease;
    private final String build;

    /**
     * A new instance of a semantic version.
     *
     * @param major      The major version.
     * @param minor      The minor version.
     * @param patch      The patch version.
     * @param prerelease The prerelease version (e.g. "alpha").
     * @param build      The build metadata (e.g. "nightly.232").
     */
    private SemanticVersion(int major, int minor, int patch, @Nullable String prerelease, @Nullable String build) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.prerelease = prerelease == null ? "" : prerelease;
        this.build = build == null ? "" : build;
    }

    public Tuple5<Integer, Integer, Integer, String, String> deconstruct() {
        return Tuples.of(this.major, this.minor, this.patch, this.prerelease, this.build);
    }

    /**
     * A new instance of a semantic version.
     *
     * @param major The major version.
     */
    public static SemanticVersion of(int major) {
        return new SemanticVersion(major, 0, 0, null, null);
    }

    /**
     * A new instance of a semantic version.
     *
     * @param major The major version.
     * @param minor The minor version.
     */
    public static SemanticVersion of(int major, int minor) {
        return new SemanticVersion(major, minor, 0, null, null);
    }

    /**
     * A new instance of a semantic version.
     *
     * @param major The major version.
     * @param minor The minor version.
     * @param patch The patch version.
     */
    public static SemanticVersion of(int major, int minor, int patch) {
        return new SemanticVersion(major, minor, patch, null, null);
    }

    /**
     * A new instance of a semantic version.
     *
     * @param major      The major version.
     * @param minor      The minor version.
     * @param patch      The patch version.
     * @param prerelease The prerelease version (e.g. "alpha").
     */
    public static SemanticVersion of(int major, int minor, int patch, @Nullable String prerelease) {
        return new SemanticVersion(major, minor, patch, prerelease, null);
    }

    /**
     * A new instance of a semantic version.
     *
     * @param major      The major version.
     * @param minor      The minor version.
     * @param patch      The patch version.
     * @param prerelease The prerelease version (e.g. "alpha").
     * @param build      The build metadata (e.g. "nightly.232").
     */
    public static SemanticVersion of(int major, int minor, int patch, @Nullable String prerelease, @Nullable String build) {
        return new SemanticVersion(major, minor, patch, prerelease, build);
    }

    public int getMajor() {
        return this.major;
    }

    public int getMinor() {
        return this.minor;
    }

    public int getPatch() {
        return this.patch;
    }

    public String getPrerelease() {
        return this.prerelease;
    }

    public String getBuild() {
        return this.build;
    }

    /**
     * Compares this semantic version to another one.
     * @param other The other value.
     * @return If this is equal to other {@code true}, otherwise {@code false}.
     */
    public boolean isEqualTo(SemanticVersion other) {
        return compareTo(other) == 0;
    }

    /**
     * Compares this semantic version to another one.
     * @param other The other value.
     * @return If this is not equal to other {@code true}, otherwise {@code false}.
     */
    public boolean isNotEqualTo(SemanticVersion other) {
        return !isEqualTo(other);
    }

    /**
     * Compares this semantic version to another one.
     * @param other The other value.
     * @return If this is greater than other {@code true}, otherwise {@code false}.
     */
    public boolean isGreaterThan(SemanticVersion other) {
        return compareTo(other) > 0;
    }

    /**
     * Compares this semantic version to another one.
     * @param other The other value.
     * @return If this is greater than or equal to other {@code true}, otherwise {@code false}.
     */
    public boolean isGreaterOrEqualTo(SemanticVersion other) {
        return isEqualTo(other) || isGreaterThan(other);
    }

    /**
     * Compares this semantic version to another one.
     * @param other The other value.
     * @return If this is less than other {@code true}, otherwise {@code false}.
     */
    public boolean isLessThan(SemanticVersion other) {
        return compareTo(other) < 0;
    }

    /**
     * Compares this semantic version to another one.
     * @param other The other value.
     * @return if this is less than or equal to other {@code true}, otherwise {@code false}.
     */
    public boolean isLessOrEqualTo(SemanticVersion other) {
        return isEqualTo(other) || isLessThan(other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SemanticVersion that = (SemanticVersion) o;
        return major == that.major
                && minor == that.minor
                && patch == that.patch
                && Objects.equals(prerelease, that.prerelease)
                && Objects.equals(build, that.build);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, prerelease, build);
    }

    /**
     * Compares the current instance with another object of the same type and returns an integer that indicates
     * whether the current instance precedes, follows, or occurs in the same position in the sort order as the
     * other object.
     * @param other An object to compare with this instance.
     * @return
     * A value that indicates the relative order of the objects being compared.
     * The return value has these meanings:
     *   Less than zero: This instance precedes {@code other} in the sort order.
     *   Zero: This instance occurs in the same position in the sort order as {@code other}.
     *   Greater than zero: This instance follows {@code other} in the sort order.
     */
    @Override
    public int compareTo(@NotNull SemanticVersion other) {
        return ComparisonChain.start()
                .compare(this.major, other.major)
                .compare(this.minor, other.minor)
                .compare(this.patch, other.patch)
                .compare(this.prerelease, other.prerelease)
                .compare(this.build, other.build)
                .result();
    }

    /**
     * @return the {@code String} equivalent of this version
     */
    @Override
    public String toString() {
        var builder = new StringBuilder();
        builder.append(this.major).append(".").append(this.minor).append(".").append(this.patch);
        if (!Strings.isEmptyOrNull(this.prerelease)) {
            builder.append("-").append(this.prerelease);
        }
        if (!Strings.isEmptyOrNull(this.build)) {
            builder.append("-").append(this.build);
        }
        return builder.toString();
    }

    /**
     * Converts the string representation of a semantic version to its @see {@link SemanticVersion} equivalent.
     *
     * @param version the version string.
     * @return a valid @see {@link SemanticVersion} object.
     * @throws NullPointerException     if {@code version} is {@code null}
     * @throws IllegalArgumentException if {@code version} has an invalid format.
     * @throws IllegalArgumentException if {@code version} is missing {@code minor} or {@code patch} versions and {@code strict} is {@code true}.
     * @throws NumberFormatException    if {@code major}, {@code minor}, or {@code patch} versions cannot be parsed to Integer.
     */
    public static SemanticVersion parse(String version) {
        return parse(version, false);
    }

    /**
     * Converts the string representation of a semantic version to its @see {@link SemanticVersion} equivalent.
     *
     * @param version the version string.
     * @param strict  if set to @see {@code true} minor and patch version are required, otherwise they are optional.
     * @return a valid @see {@link SemanticVersion} object.
     * @throws NullPointerException     if {@code version} is {@code null}
     * @throws IllegalArgumentException if {@code version} has an invalid format.
     * @throws IllegalArgumentException if {@code version} is missing {@code minor} or {@code patch} versions and {@code strict} is {@code true}.
     * @throws NumberFormatException    if {@code major}, {@code minor}, or {@code patch} versions cannot be parsed to Integer.
     */
    public static SemanticVersion parse(String version, boolean strict) {
        Objects.requireNonNull(version, "Expected a non-null version, got null.");
        Matcher matcher = SemanticVersion.ParseExpression.matcher(version);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format(
                    "Invalid version: '%s', does not match: '%s'", version, ParseExpression.pattern()
            ));
        }
        var major = Optional.ofNullable(matcher.group(ParseExpressionGroupMajor))
                .map(Integer::parseInt)
                .orElseThrow();
        var minor = Optional.ofNullable(matcher.group(ParseExpressionGroupMinor)).map(Integer::parseInt);
        if (strict && minor.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Invalid version: '%s', no minor version given in strict mode", version
            ));
        }
        var patch = Optional.ofNullable(matcher.group(ParseExpressionGroupPatch)).map(Integer::parseInt);
        if (strict && patch.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Invalid version: '%s', no patch version given in strict mode", version
            ));
        }
        var prerelease = Optional.ofNullable(matcher.group(ParseExpressionGroupPre));
        var build = Optional.ofNullable(matcher.group(ParseExpressionGroupBuild));
        return new SemanticVersion(
                major, minor.orElse(0), patch.orElse(0),
                prerelease.orElse(""), build.orElse("")
        );
    }
}
