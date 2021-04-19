package com.tencent.devops.experience.constant

enum class ExperienceConditionEnum(
    val id: Int
) {
    UNKNOWN(0),

    JUST_PUBLIC(1),

    JUST_PRIVATE(2),

    BOTH_WITH_PRIVATE(3),

    BOTH_WITHOUT_PRIVATE(4),

    ;
}
