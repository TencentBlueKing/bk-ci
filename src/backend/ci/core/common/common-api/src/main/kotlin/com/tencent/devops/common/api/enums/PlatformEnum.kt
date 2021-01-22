package com.tencent.devops.common.api.enums

enum class PlatformEnum(
    val id: Int,
    val mean: String
) {
    ANDROID(1, "安卓"),

    IOS(2, "IOS"),

    ;

    companion object {
        fun of(id: Int?): PlatformEnum? {
            if (null == id) {
                return null
            }

            values().forEach {
                if (it.id == id) {
                    return it
                }
            }

            return null
        }
    }
}
