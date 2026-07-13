package com.tencent.devops.remotedev.pojo

enum class FeatureSwitchType {
    LIVE_STREAMING,
    LIVE_STREAMING_DEVCLOUD,
    ;

    companion object {
        fun parse(value: String): FeatureSwitchType {
            return entries.find { it.name == value }
                ?: throw IllegalArgumentException(
                    "Unknown FeatureSwitchType: $value"
                )
        }
    }
}
