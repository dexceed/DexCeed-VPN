package app.rawline.mobile.group

import java.net.URI

/** Only our HTTPS subscription URLs opt into managed delivery; never match a suffix or user-info. */
object DexceedSubscriptionUrl {
    private val hosts = setOf("sub.dexceed.online", "submainru.dexceed.online", "panel.dexceed.online")
    private val token = Regex("[A-Za-z0-9_-]{8,128}")
    fun canonical(value: String): String? {
        val uri = runCatching { URI(value.trim()) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", true) || uri.host?.lowercase() !in hosts || uri.rawUserInfo != null ||
            uri.port !in listOf(-1,443) || uri.rawQuery != null) return null
        val path = uri.rawPath.trim('/').split('/')
        val id = when {
            path.size == 1 -> path[0]
            path.size == 2 && path[1] in setOf("singbox", "base64", "v2ray", "xray") -> path[0]
            path.size == 2 && path[0] == "sub" -> path[1]
            path.size in 3..4 && path.take(2) == listOf("api","sub") && (path.size == 3 || path[3] in setOf("singbox","base64","v2ray","xray")) -> path[2]
            path.size == 3 && path[0] == "dexceed" && path[2] == "subscription" -> path[1]
            else -> return null
        }
        if (!token.matches(id)) return null
        return "https://sub.dexceed.online/dexceed/$id/subscription"
    }
}
