package com.mju.onda.driver.feature.settings.data

data class StopRequestDraft(
    val reason: String,
    val routeName: String,
    val vehicleName: String,
    val locationLabel: String,
    val includeLocation: Boolean,
    val message: String,
    val contactable: Boolean,
)

object StopRequestDraftHolder {
    @Volatile
    var draft: StopRequestDraft? = null
        private set

    fun set(draft: StopRequestDraft) {
        this.draft = draft
    }

    fun clear() {
        draft = null
    }
}
