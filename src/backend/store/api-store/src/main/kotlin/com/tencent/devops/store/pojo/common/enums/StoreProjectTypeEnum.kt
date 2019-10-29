package com.tencent.devops.store.pojo.common.enums

enum class StoreProjectTypeEnum(val type: Int) {
    INIT(0), // 新增插件时关联的初始化项目
    COMMON(1), // 安装插件时关联的项目
    TEST(2); // 申请为插件协作者时关联的调试项目

    companion object {
        fun getProjectType(type: Int): String {
            return when (type) {
                0 -> INIT.name
                1 -> COMMON.name
                2 -> TEST.name
                else -> COMMON.name
            }
        }
    }
}