package app.rawline.mobile.group
import org.junit.Assert.*
import org.junit.Test
class DexceedSubscriptionUrlTest {
    @Test fun canonicalizesAllPersonalEntryPoints() {
        val expected="https://sub.dexceed.online/dexceed/person_123/subscription"
        for (url in listOf("https://sub.dexceed.online/person_123", "https://submainru.dexceed.online/person_123/singbox", "https://panel.dexceed.online/api/sub/person_123/singbox", expected)) assertEquals(expected,DexceedSubscriptionUrl.canonical(url))
    }
    @Test fun refusesUnrelatedMalformedOrCredentialBearingUrls() {
        for (url in listOf("http://sub.dexceed.online/person_123", "https://sub.dexceed.online.evil.test/person_123", "https://user@sub.dexceed.online/person_123", "https://sub.dexceed.online:444/person_123", "https://sub.dexceed.online/person_123?target=elsewhere", "https://sub.dexceed.online/%2fperson_123", "https://other.example/person_123", "https://sub.dexceed.online/dexceed/setup")) assertNull(url,DexceedSubscriptionUrl.canonical(url))
    }
    @Test fun keepsAccountsSeparateAndCanonicalizationIdempotent() {
        val first=DexceedSubscriptionUrl.canonical("https://sub.dexceed.online/person_123")!!
        assertEquals(first,DexceedSubscriptionUrl.canonical(first))
        assertNotEquals(first,DexceedSubscriptionUrl.canonical("https://sub.dexceed.online/person_456"))
    }
}
