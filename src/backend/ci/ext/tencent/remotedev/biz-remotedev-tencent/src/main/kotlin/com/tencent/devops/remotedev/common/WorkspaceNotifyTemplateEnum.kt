package com.tencent.devops.remotedev.common

enum class WorkspaceNotifyTemplateEnum(val templateCode: String) {
    /**
     * 流水线设置-启动的通知模板代码
     */
    REMOTEDEV_WORKSPACE_RECYCLE_TEMPLATE("REMOTEDEV_WORKSPACE_RECYCLE_TEMPLATE"),

    /**
     * 未知模板代码
     */
    UNKNOWN("NULL");

    companion object {

        fun parse(name: String?): WorkspaceNotifyTemplateEnum {
            return try {
                if (name == null) UNKNOWN else valueOf(name)
            } catch (ignored: Exception) {
                UNKNOWN
            }
        }
    }
}
