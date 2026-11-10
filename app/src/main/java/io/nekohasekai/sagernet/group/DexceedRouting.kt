package app.rawline.mobile.group

import app.rawline.mobile.database.SubscriptionOptions

/** An owned single-source Auto uses its source policy; explicit user choices always win. */
object DexceedRouting {
    fun inherited(own: SubscriptionOptions?, sourceUrls: List<String>, source: SubscriptionOptions?): SubscriptionOptions? {
        if (own != null && (own.routeProfileId > 0 || own.routingProvider.isNotBlank())) return own
        if (sourceUrls.size != 1 || DexceedSubscriptionUrl.canonical(sourceUrls.single()) == null) return own
        return source ?: own
    }
}
