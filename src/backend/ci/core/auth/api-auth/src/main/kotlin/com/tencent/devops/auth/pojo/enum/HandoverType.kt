package com.tencent.devops.auth.pojo.enum

enum class HandoverType(val value: String, val alias: String) {
    // 用户组
    GROUP("group", "用户组"),

    // 授权
    AUTHORIZATION("authorization", "授权管理");

    companion object {
        fun get(value: String): HandoverType {
            HandoverType.values().forEach {
                if (value == it.value) return it
            }
            throw IllegalArgumentException("No enum for constant $value")
        }
    }
}
