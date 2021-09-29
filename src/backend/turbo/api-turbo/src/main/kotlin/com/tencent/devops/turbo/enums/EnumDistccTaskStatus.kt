package com.tencent.devops.turbo.enums

enum class EnumDistccTaskStatus(private val statusName: String) {
    STAGING("准备中"),
    STARTING("正在开始"),
    RUNNING("正在构建"),
    FAILED("构建失败"),
    FINISH("构建完成");

    fun getTBSStatus(): String {
        return this.name.toLowerCase()
    }

    fun getStatusName(): String {
        return this.statusName
    }
}
