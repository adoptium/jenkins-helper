/*
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package common

import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNull

class VersionInfoTest {

    /** Stand-in for the Jenkins pipeline context, swallows the logging. */
    static class QuietContext {
        void println(Object message) {}
    }

    private static VersionInfo parse(String publishName, String adoptBuildNumber = "") {
        return new VersionInfo(new QuietContext()).parse(publishName, adoptBuildNumber)
    }

    // ---------------------------------------------------------------- JDK8 and earlier

    @Test
    void jdk8Release() {
        VersionInfo v = parse("jdk8u272-b10")
        assertEquals(8, v.major)
        assertEquals(0, v.minor)
        assertEquals(272, v.security)
        assertEquals(10, v.build)
        assertNull(v.patch)
        assertNull(v.opt)
        assertNull(v.pre)
        assertNull(v.additional_version_numbers)
        assertEquals("8u272-b10", v.version)
        assertEquals("8.0.272+10", v.semver)
        assertEquals("8.0.272.10", v.msi_product_version)
    }

    @Test
    void jdk8ReleaseWithAdoptBuildNumber() {
        VersionInfo v = parse("jdk8u272-b10", "2")
        assertEquals(2, v.adopt_build_number)
        assertEquals("8.0.272+10.2", v.semver)
    }

    @Test
    void jdk8ReleaseWithOpt() {
        VersionInfo v = parse("jdk8u272-b10_202010231715")
        assertEquals(272, v.security)
        assertEquals(10, v.build)
        assertEquals("202010231715", v.opt)
        // opt present so adopt_build_number is padded with 0
        assertEquals(0, v.adopt_build_number)
        assertEquals("8.0.272+10.0.202010231715", v.semver)
    }

    @Test
    void jdk8DottedRelease() {
        VersionInfo v = parse("1.8.0_272-b10")
        assertEquals(8, v.major)
        assertEquals(0, v.minor)
        assertEquals(272, v.security)
        assertEquals(10, v.build)
        assertNull(v.pre)
        assertEquals("1.8.0_272-b10", v.version)
        assertEquals("8.0.272+10", v.semver)
    }

    @Test
    void jdk8DottedReleaseWithPre() {
        VersionInfo v = parse("1.8.0_272-ea-b10")
        assertEquals(8, v.major)
        assertEquals(272, v.security)
        assertEquals(10, v.build)
        assertEquals("ea", v.pre)
        assertEquals("8.0.272-ea+10", v.semver)
    }

    @Test
    void jdk8Nightly() {
        VersionInfo v = parse("1.8.0_202-internal-201903130451-b08")
        assertEquals(8, v.major)
        assertEquals(202, v.security)
        assertEquals(8, v.build)
        assertEquals("internal", v.pre)
        assertEquals("201903130451", v.opt)
        assertEquals("8.0.202-internal+8.0.201903130451", v.semver)
    }

    // ---------------------------------------------------------------- JDK9+

    @Test
    void featureReleaseOnly() {
        VersionInfo v = parse("jdk-15+36")
        assertEquals(15, v.major)
        assertEquals(0, v.minor)
        assertEquals(0, v.security)
        assertEquals(36, v.build)
        assertNull(v.patch)
        assertNull(v.additional_version_numbers)
        assertEquals("15+36", v.version)
        assertEquals("15.0.0+36", v.semver)
        assertEquals("15.0.0.36", v.msi_product_version)
    }

    @Test
    void threeNumberRelease() {
        VersionInfo v = parse("jdk-11.0.9+11")
        assertEquals(11, v.major)
        assertEquals(0, v.minor)
        assertEquals(9, v.security)
        assertEquals(11, v.build)
        assertNull(v.patch)
        assertEquals("11.0.9+11", v.version)
        assertEquals("11.0.9+11", v.semver)
        assertEquals("11.0.9.11", v.msi_product_version)
    }

    @Test
    void releaseWithoutJdkPrefix() {
        VersionInfo v = parse("17.0.1+12")
        assertEquals(17, v.major)
        assertEquals(1, v.security)
        assertEquals(12, v.build)
        assertEquals("17.0.1+12", v.semver)
    }

    @Test
    void releaseWithAdoptBuildNumber() {
        VersionInfo v = parse("jdk-17.0.1+12", "3")
        assertEquals(3, v.adopt_build_number)
        assertEquals("17.0.1+12.3", v.semver)
    }

    @Test
    void fourNumberReleaseUsesPatch() {
        VersionInfo v = parse("jdk-11.0.9.1+1")
        assertEquals(9, v.security)
        assertEquals(1, v.patch)
        assertEquals(1, v.build)
        assertNull(v.additional_version_numbers)
        assertEquals("11.0.9.1+1", v.version)
        // build 1 + patch 1 x 100000000
        assertEquals("11.0.9+100000001", v.semver)
        // build 1 + patch 1 x 10000
        assertEquals("11.0.9.10001", v.msi_product_version)
    }

    @Test
    void zeroPatchDoesNotChangeSemver() {
        VersionInfo v = parse("jdk-11.0.9.0+11")
        assertEquals(0, v.patch)
        assertEquals("11.0.9+11", v.semver)
    }

    @Test
    void earlyAccessRelease() {
        VersionInfo v = parse("jdk-11.0.9-ea+11")
        assertEquals(9, v.security)
        assertEquals(11, v.build)
        assertEquals("ea", v.pre)
        assertEquals("11.0.9-ea+11", v.semver)
        assertEquals("11.0.9-ea", v.formOpenjdkSemver())
    }

    @Test
    void releaseWithTimestampOpt() {
        VersionInfo v = parse("jdk-11.0.9+11-202011050024")
        assertEquals(11, v.build)
        assertEquals("202011050024", v.opt)
        assertEquals(0, v.adopt_build_number)
        assertEquals("11.0.9+11.0.202011050024", v.semver)
    }

    @Test
    void releaseWithAdhocOpt() {
        VersionInfo v = parse("jdk-11.0.9+11-adhoc.username-myfolder")
        assertEquals(11, v.build)
        assertEquals("adhoc.username-myfolder", v.opt)
        assertEquals("11.0.9+11.0.adhoc.username-myfolder", v.semver)
    }

    @Test
    void preWithoutBuild() {
        VersionInfo v = parse("jdk-21-ea")
        assertEquals(21, v.major)
        assertEquals("ea", v.pre)
        assertEquals(0, v.build)
        assertEquals("21.0.0-ea+0", v.semver)
    }

    // ---------------------------------------------------------------- Extra version numbers (issue #66)

    @Test
    void fiveNumberRelease() {
        VersionInfo v = parse("jdk-27.0.1.0.2+1")
        assertEquals(27, v.major)
        assertEquals(0, v.minor)
        assertEquals(1, v.security)
        assertEquals(0, v.patch)
        assertEquals([2], v.additional_version_numbers)
        assertEquals(1, v.build)
        assertEquals("27.0.1.0.2+1", v.version)
        // build 1 + additional1 2 x 1000000
        assertEquals("27.0.1+2000001", v.semver)
        // build 1 + additional1 2 x 1000
        assertEquals("27.0.1.2001", v.msi_product_version)
    }

    @Test
    void sixNumberRelease() {
        VersionInfo v = parse("jdk-27.0.1.0.2.3+1")
        assertEquals([2, 3], v.additional_version_numbers)
        // build 1 + additional1 2 x 1000000 + additional2 3 x 10000
        assertEquals("27.0.1+2030001", v.semver)
        // build 1 + additional1 2 x 1000 + additional2 3 x 100
        assertEquals("27.0.1.2301", v.msi_product_version)
    }

    /**
     * Regression test for issue #66: with more than 4 version numbers the regex failed to reach the
     * build group, so build was left at 0 instead of the number after the "+".
     */
    @Test
    void buildIsParsedForAnyNumberOfVersionFields() {
        Map<String, Integer> expectedBuilds = [
                "jdk-27+7"              : 7,
                "jdk-27.0+7"            : 7,
                "jdk-27.0.1+7"          : 7,
                "jdk-27.0.1.0+7"        : 7,
                "jdk-27.0.1.0.2+7"      : 7,
                "jdk-27.0.1.0.2.3+7"    : 7,
                "jdk-27.0.1.0.2.3.4+7"  : 7,
                "jdk-27.0.1.0.2+1"      : 1,
                "jdk-27.0.1.0.2+42"     : 42,
                "jdk-27.0.1.0.2-ea+7"   : 7,
                "jdk-27.0.1.0.2+7-202611050024": 7
        ]
        expectedBuilds.each { String versionString, Integer expectedBuild ->
            assertEquals(expectedBuild, parse(versionString).build, "build for ${versionString}")
        }
    }

    @Test
    void sevenNumberRelease() {
        VersionInfo v = parse("jdk-27.0.1.1.2.3.4+5")
        assertEquals(1, v.patch)
        assertEquals([2, 3, 4], v.additional_version_numbers)
        assertEquals(5, v.build)
        // build 5 + patch 1 x 100000000 + additional1 2 x 1000000 + additional2 3 x 10000 + additional3 4 x 100
        assertEquals("27.0.1+102030405", v.semver)
        // build 5 + patch 1 x 10000 + additional1 2 x 1000 + additional2 3 x 100, additional3 (4) is not represented
        assertEquals("27.0.1.12305", v.msi_product_version)
    }

    @Test
    void fiveNumberReleaseWithPreAndOpt() {
        VersionInfo v = parse("jdk-27.0.1.0.2-ea+1-202611050024", "4")
        assertEquals([2], v.additional_version_numbers)
        assertEquals("ea", v.pre)
        assertEquals(1, v.build)
        assertEquals("202611050024", v.opt)
        assertEquals(4, v.adopt_build_number)
        assertEquals("27.0.1-ea+2000001.4.202611050024", v.semver)
    }

    @Test
    void semverBuildOrdersByVersionNumberFields() {
        // Earlier version number fields are more significant than later ones, listed in true version order
        List<String> ascending = [
                "jdk-27.0.1+99",
                "jdk-27.0.1.0.0.0.1+1",
                "jdk-27.0.1.0.0.1+1",
                "jdk-27.0.1.0.1+1",
                "jdk-27.0.1.0.2+1",
                "jdk-27.0.1.0.2+2",
                "jdk-27.0.1.1+1",
                "jdk-27.0.1.1.2.3.4+5",
                "jdk-27.0.1.1.3.1.1+5",
                "jdk-27.0.1.2+1"
        ]
        List<Long> builds = ascending.collect { parse(it).semver.split("\\+")[1] as Long }
        assertEquals(builds.sort(false), builds, "semver build numbers should be ascending: ${builds}")
        assertEquals(builds.unique(false).size(), builds.size(), "semver build numbers should be unique: ${builds}")
    }

    @Test
    void msiProductVersionOrdersByVersionNumberFields() {
        // additional3 (7th number) is not represented in the MSI version so is not included here
        List<String> ascending = [
                "jdk-27.0.1+99",
                "jdk-27.0.1.0.0.1+1",
                "jdk-27.0.1.0.0.5+99",
                "jdk-27.0.1.0.1+1",
                "jdk-27.0.1.0.2+1",
                "jdk-27.0.1.0.2+2",
                "jdk-27.0.1.0.5.5+99",
                "jdk-27.0.1.1+1",
                "jdk-27.0.1.1.2.3+5",
                "jdk-27.0.1.1.3.1+1",
                "jdk-27.0.1.2+1",
                "jdk-27.0.1.6.5.5+35"
        ]
        List<Long> revisions = ascending.collect { parse(it).msi_product_version.split("\\.")[3] as Long }
        assertEquals(revisions.sort(false), revisions, "MSI revisions should be ascending: ${revisions}")
        assertEquals(revisions.unique(false).size(), revisions.size(), "MSI revisions should be unique: ${revisions}")
    }

    @Test
    void msiProductVersionMaximumRevisionFitsInMSIField() {
        // patch 6, additional1 5, additional2 5, build 35 is the largest supported revision
        assertEquals("27.0.1.65535", parse("jdk-27.0.1.6.5.5+35").msi_product_version)
    }

    @Test
    void msiProductVersionRevisionIsCappedAtMaximum() {
        // Each of these would exceed 65535 and so must be capped
        [
                "jdk-27.0.1.7+1",
                "jdk-27.0.1.9+99",
                "jdk-27.0.1.6.5.5+36",
                "jdk-27.0.1.6.6+1",
                "jdk-27.0.1.0.99+1",
                "jdk-27.0.1.0.0.999+1",
                "jdk-27.0.1.99.99.99+99"
        ].each { String versionString ->
            assertEquals("27.0.1.65535", parse(versionString).msi_product_version, "MSI version for ${versionString}")
        }
    }

    @Test
    void semverOrdersAcrossSecurityVersionsRegardlessOfPatchWeighting() {
        // security is compared by semver before the build metadata, so older-format patch semvers still sort first
        List<String> ascending = ["jdk-11.0.9.1+1", "jdk-11.0.33.1+1"]
        List<String> semvers = ascending.collect { parse(it).semver }
        assertEquals(["11.0.9+100000001", "11.0.33+100000001"], semvers)
    }
}
