package app.rawline.mobile.group

import app.rawline.mobile.database.SubscriptionOptions
import org.junit.Assert.*
import org.junit.Test

class DexceedRoutingTest {
    private val source = SubscriptionOptions(routingProvider = "Happ", providerRouting = "RU policy")
    private val url = "https://sub.dexceed.online/dexceed/person_123/subscription"
    @Test fun ownedAutoInheritsSourcePolicy() {
        assertSame(source, DexceedRouting.inherited(SubscriptionOptions(), listOf(url), source))
    }
    @Test fun explicitUserRouteAndProviderWin() {
        for (own in listOf(SubscriptionOptions(routeProfileId = 42), SubscriptionOptions(routingProvider = "Incy")))
            assertSame(own, DexceedRouting.inherited(own, listOf(url), source))
    }
    @Test fun foreignAndMixedSourcesDoNotAcquireOurPolicy() {
        val own = SubscriptionOptions()
        for (urls in listOf(emptyList(), listOf("https://other.example/user"), listOf(url, url)))
            assertSame(own, DexceedRouting.inherited(own, urls, source))
    }
}
