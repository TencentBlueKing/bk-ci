package com.tencent.devops.common.api.enums

enum class PlatformEnum(
    val id: Int,
    val mean: String
) {
    UNKNOWN(-1, "未知"),

    ANDROID(1, "安卓"),

    IOS(2, "IOS"),

    ;
}