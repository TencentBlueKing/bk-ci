package com.tencent.devops.experience.constant

enum class ProductCategoryEnum(
    val id: Int,
    val mean: String
) {
    UNKNOWN(-1, "未知"),

    GAME(1, "游戏"),

    TOOL(2, "工具"),

    LIFE(3, "生活"),

    SOCIAL(4, "社交"),

    ;
}