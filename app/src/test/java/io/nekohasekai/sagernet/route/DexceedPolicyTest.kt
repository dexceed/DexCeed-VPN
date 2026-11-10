package app.rawline.mobile.route
import app.rawline.mobile.outbound.json.*
import app.rawline.mobile.outbound.config.GeneratorSettings
import org.junit.Assert.*
import org.junit.Test
class DexceedPolicyTest {
    @Test fun productionPolicyRoutesRuAndLanDirectAndEverythingElseViaProxy() {
        val text = javaClass.getResourceAsStream("/dexceed-ru-routing.json")!!.bufferedReader().use { it.readText() }
        val document = ProviderRouting.parse(text).document!!
        val route = ProviderRouting.profile(document,"Happ")
        assertEquals(OutboundIds.PROXY,route.default_outbound_id)
        val domains = route.rules.single { it.name == "DirectSites" }
        val ips = route.rules.single { it.name == "DirectIp" }
        assertEquals(OutboundIds.DIRECT,domains.outbound_id)
        assertEquals(OutboundIds.DIRECT,ips.outbound_id)
        assertTrue(domains.rule_set.containsAll(listOf("geosite:private","geosite:category-ru")))
        assertTrue(domains.domain_suffix.containsAll(listOf("ru","su","xn--p1ai")))
        assertTrue(ips.rule_set.containsAll(listOf("geoip:private","geoip:ru")))
        val resolve = route.rules.indexOfFirst { it.action == "resolve" }
        assertTrue(resolve > route.rules.indexOf(domains))
        assertTrue(resolve < route.rules.indexOf(ips))
        val dns = ProviderRouting.settings(document,GeneratorSettings())
        assertEquals("local",dns.directDns)
        assertTrue(dns.remoteDns.startsWith("https://"))
        assertFalse(dns.fakeDns)
    }
}
