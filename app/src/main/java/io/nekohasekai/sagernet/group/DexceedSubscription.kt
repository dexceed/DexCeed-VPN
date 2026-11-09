package app.rawline.mobile.group

import app.rawline.mobile.database.*
import app.rawline.mobile.outbound.types.AutoSelector
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Per-account native selectors. No fixed local IDs, credentials or destructive backup restores. */
object DexceedSubscription {
    private val writes = Mutex()
    private fun key(gid: Long) = "dexceed_auto_group_v1_$gid"

    suspend fun subscribe(url: String, name: String): Long = writes.withLock {
        val source = GroupRepo.all().filter { !it.archive && DexceedSubscriptionUrl.canonical(it.url) == url }
            .sortedBy { if (it.url == url) 0 else 1 }.firstOrNull()
            ?: GroupRepo.add(ProxyGroup(name = name.ifBlank { "Серверы Dexceed" }, url = url))
        val store = DataStore.configurationStore
        val stored = store.getString(key(source.id), "")?.toLongOrNull()?.let { GroupRepo.get(it) }
        val existing = stored?.takeIf { !it.isSubscription && !it.archive } ?: GroupRepo.all().firstOrNull { group ->
            !group.isSubscription && !group.archive && ProfileManager.members(group.id).count {
                val selector = it.outbound as? AutoSelector
                selector != null && selector.gids == listOf(source.id) && it.name in listOf("Dexceed Auto", "Dexceed HTTPS")
            } == 2
        }
        val display = existing ?: GroupRepo.add(ProxyGroup(name = if (GroupRepo.all().none { it.name == "Dexceed Auto" }) "Dexceed Auto" else "Dexceed Auto · ${source.name}"))
        if (stored == null) {
            source.url = url
            source.skipAutoUpdate = false
            source.subOptions = source.subOptions.copy(sendHwid = SendHwid.ON, autoUpdateMinutes = 30,
                requestTimeoutSeconds = 10, allowInsecureRequests = false, providerIgnoreImport = true)
            GroupRepo.save(source)
        }
        store.putString(key(source.id), display.id.toString())
        val members = ProfileManager.members(display.id)
        for ((title, pattern) in listOf("Dexceed Auto" to "^Dexceed · (HY2|HTTPS) · ", "Dexceed HTTPS" to "^Dexceed · HTTPS · ")) {
            if (members.any { it.name == title && (it.outbound as? AutoSelector)?.gids == listOf(source.id) }) continue
            ProfileManager.addProfile(AutoSelector().apply {
                this.name = title; gids = arrayListOf(source.id); nameFilter = pattern; excludeUnavailable = false
                poolCap = 128; buildLimit = 64; resultValidityMins = 10
                testURL = "https://www.gstatic.com/generate_204"; intervalSec = 30; benchIntervalSec = 60
                watchIntervalSec = 5; activeSize = 8; sampling = 3; toleranceMs = 80; expected = 2
                interruptOnSwitch = false
            }, display.id)
        }
        store.putString(key(source.id), display.id.toString())
        DataStore.mobileListMode = "groups"
        DataStore.selectedProxy = ProfileManager.members(display.id).first { it.name == "Dexceed Auto" }.id
        SubscriptionScheduler.schedule()
        SubscriptionQueue.refreshGroup(source.id, showDiff = false, foreground = true)
        display.id
    }

    /** Upgrade existing accounts once; do not delete old profiles or change another provider's current selection. */
    suspend fun migrateExisting() {
        val store = DataStore.configurationStore
        if (store.getBoolean("dexceed_managed_subscriptions_v1", false)) return
        val current = GroupRepo.currentId()
        val previousSelected = DataStore.selectedProxy
        var selectedReplacement: Long? = null
        val sources = GroupRepo.all().filter { !it.archive && DexceedSubscriptionUrl.canonical(it.url) != null }
            .sortedBy { if (it.url == DexceedSubscriptionUrl.canonical(it.url)) 0 else 1 }
            .distinctBy { DexceedSubscriptionUrl.canonical(it.url) }
        for (source in sources) {
            val display = SubscriptionClient.subscribeUrl(source.url, source.name, true)
            if (display > 0 && current == source.id) {
                GroupRepo.setCurrent(display)
                selectedReplacement = DataStore.selectedProxy
            }
        }
        DataStore.selectedProxy = selectedReplacement ?: previousSelected
        store.putBoolean("dexceed_managed_subscriptions_v1", true)
    }
}
