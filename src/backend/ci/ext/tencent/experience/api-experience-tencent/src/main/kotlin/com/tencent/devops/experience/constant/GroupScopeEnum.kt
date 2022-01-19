package com.tencent.devops.experience.constant

/**
 * 体验范围
 */
enum class GroupScopeEnum(
    val id: Int,
    val mean: String
) {
    PUBLIC(0, "公开体验"),

    PRIVATE(1, "内部体验"),

    ;
}
