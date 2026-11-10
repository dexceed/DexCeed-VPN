package app.rawline.mobile.update

import org.junit.Assert.*
import org.junit.Test

class SemverTagTest {
    @Test fun dexceedTagsRemainCompatible() {
        assertNull(Semver.ofTag("rawline-1.1.1"))
        assertNull(Semver.ofTag("dev"))
        val installed = Semver.ofTag("v1.0.1")!!
        assertTrue(Semver.compare(Semver.ofTag("v1.1.0")!!, installed) > 0)
        assertTrue(Semver.compare(Semver.ofTag("v1.1.0-beta.1")!!, installed) > 0)
    }
}
