package com.tencent.devops.experience.constant

enum class ExperienceConditionEnum(
    val id: Int
) {
    UNKNOWN(0),

    // 体验范围仅是公开体验
    JUST_PUBLIC(1),

    // 体验范围仅是内部体验
    JUST_PRIVATE(2),

    // 体验范围同时包括公开体验和内部体验组 , 用户属于内部体验组
    BOTH_WITH_PRIVATE(3),

    // 体验范围同时包括公开体验和内部体验组 , 用户不在内部体验组
    BOTH_WITHOUT_PRIVATE(4),

    ;
}
