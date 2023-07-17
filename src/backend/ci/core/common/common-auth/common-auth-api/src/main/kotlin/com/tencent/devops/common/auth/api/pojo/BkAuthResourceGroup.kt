package com.tencent.devops.common.auth.api.pojo

enum class BkAuthResourceGroup(
    val value: String,
    val groupName: String
) {
    MANAGER("manager", "管理员"), // 管理员
    EDITOR("editor", "编辑者"), // 编辑者
    EXECUTOR("executor", "执行者"), // 执行者
    VIEWER("viewer", "查看者"); // 查看者

    companion object {
        fun get(value: String): BkAuthGroup {
            BkAuthGroup.values().forEach {
                if (value == it.value) return it
            }
            throw IllegalArgumentException("No enum for constant $value")
        }

        fun contains(value: String): Boolean {
            BkAuthGroup.values().forEach {
                if (value == it.value) return true
            }
            return false
        }
    }
}
