package com.mju.onda.driver.feature.settings.data

data class StopRequestReceivedInfo(
    val requestId: String = "",
    val reason: String,
    val requestedAt: String,
)

object StopRequestReceivedHolder {
    @Volatile
    var info: StopRequestReceivedInfo? = null
        private set

    fun set(info: StopRequestReceivedInfo) {
        this.info = info
    }

    fun clear() {
        info = null
    }
}
