package com.tencent.devops.auth.pojo.enum

enum class UserStatus(
    val id: Int,
    val mean: String
) {
    NORMAL(0, "正常"),

    FREEZE(1, "冻结"),

    ;
}
