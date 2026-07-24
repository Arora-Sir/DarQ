package com.kieronquinn.app.darq.model.xposed

data class XposedSettings(
    val enabled: Boolean,
    val isScheduleBlocking: Boolean = false,
    val aggressiveDark: Boolean? = null,
    val invertStatus: Boolean? = null
){
    override fun toString(): String {
        return "XposedSettings enabled $enabled isScheduleBlocking $isScheduleBlocking aggressiveDark $aggressiveDark $invertStatus"
    }
}
