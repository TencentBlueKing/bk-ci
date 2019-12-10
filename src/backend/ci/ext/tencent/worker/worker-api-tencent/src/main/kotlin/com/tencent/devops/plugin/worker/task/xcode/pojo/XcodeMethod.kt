package com.tencent.devops.plugin.worker.task.xcode.pojo

enum class XcodeMethod(val type: String) {
    DEVELOPMENT("development"),
    ENTERPRISE("enterprise"),
    APP_STORE("app-store"),
    AD_HOC("ad-hoc"),
    PACKAGE("package"),
    DEVELOPMENT_ID("development-id"),
    MAC_APP("mac-application");

    companion object {
        fun parse(type: String): XcodeMethod {
            when (type) {
                DEVELOPMENT.type -> return DEVELOPMENT
                ENTERPRISE.type -> return ENTERPRISE
                APP_STORE.type -> return APP_STORE
                AD_HOC.type -> return AD_HOC
                PACKAGE.type -> return PACKAGE
                DEVELOPMENT_ID.type -> return DEVELOPMENT_ID
                MAC_APP.type -> return MAC_APP
            }
            return DEVELOPMENT
        }
    }
}