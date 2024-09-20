package com.tencent.devops.remotedev.pojo

/**
 * 升级的不同种类，整合下方便收归
 */
enum class ClientUpgradeKind(val value: String) {
    // 正常升级，当前最新版本
    CURRENT_VERSION("verison"),

    // 正常升级，每次升级的最大数量
    MAX_NUMB("version.maxnumb"),

    // 指定升级，指定用户和版本
    CURRENT_USER_VERSION("currentuser.version"),

    // 指定升级，指定工作空间名称和版本
    CURRENT_WORKSPACE_NAME_VERSION("workspace.name.version"),

    // 指定升级，指定的项目和版本
    CURRENT_PROJECT_VERSION("project.version")
}

/**
 * 升级的不同组件，整合下方便收归
 */
enum class ClientUpgradeComp(val value: String) {
    // 蓝盾客户端
    CLIENT("client"),

    // start 客户端
    START("start")
}