package com.rstube.app

import org.junit.Assert.*
import org.junit.Test

class UpdatePolicyTest {
    private val release = AppRelease(3, "0.3.0",
        "https://github.com/rajibsarder72942/RS-Tube/releases/download/v0.3.0/RS-Tube-v0.3.0.apk", "a".repeat(64), 100)

    @Test fun onlyHigherVersionCodesAreUpdates() {
        assertTrue(UpdatePolicy.isNewer(release, 2))
        assertFalse(UpdatePolicy.isNewer(release, 3))
        assertFalse(UpdatePolicy.isNewer(release, 4))
    }
    @Test fun acceptsThePublishersVersionedAsset() { assertEquals(release, UpdatePolicy.validate(release)) }
    @Test fun rejectsForeignAndInsecureAssets() {
        for (url in listOf(release.url.replace("https:", "http:"), release.url.replace("github.com", "github.com.evil.test"),
            release.url.replace("rajibsarder72942", "someone-else"), release.url.replace("v0.3.0/", "v0.2.0/"),
            release.url + "?redirect=bad")) {
            try { UpdatePolicy.validate(release.copy(url = url)); fail(url) } catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun rejectsMissingHashAndUnboundedSize() {
        for (bad in listOf(release.copy(sha256 = ""), release.copy(sha256 = "g".repeat(64)),
            release.copy(bytes = 0), release.copy(bytes = UpdatePolicy.MAX_BYTES + 1), release.copy(code = -1))) {
            try { UpdatePolicy.validate(bad); fail(bad.toString()) } catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun refusesAChangedOrMissingSigningCertificate() {
        assertTrue(UpdatePolicy.sameSigners(setOf("trusted"), setOf("trusted")))
        assertFalse(UpdatePolicy.sameSigners(setOf("trusted"), setOf("attacker")))
        assertFalse(UpdatePolicy.sameSigners(emptySet(), emptySet()))
        assertFalse(UpdatePolicy.sameSigners(setOf("trusted"), setOf("trusted", "extra")))
    }
}
