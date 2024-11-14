package com.tencent.devops.auth.pojo.enum

enum class HandoverType(val value: String) {
    // 用户组
    GROUP("group"),

    // 授权
    AUTHORIZATION("authorization");

    companion object {
        fun get(value: String): HandoverType {
            HandoverType.values().forEach {
                if (value == it.value) return it
            }
            throw IllegalArgumentException("No enum for constant $value")
        }
    }
}
