package com.tencent.devops.remotedev.pojo.job

enum class JobType {
    ONCE,
    CRON
}

enum class JobActionType {
    NOTIFY_REMOTEDEV_DESKTOP,
    CRON_POWER_ON,
    PIPELINE;

    companion object {
        fun fromStr(v: String): JobActionType? {
            return values().firstOrNull { it.name == v }
        }

        const val NOTIFY_REMOTEDEV_DESKTOP_CONST_NAME = "NOTIFY_REMOTEDEV_DESKTOP"
        const val CRON_POWER_ON_CONST_NAME = "CRON_POWER_ON"
        const val PIPELINE_CONST_NAME = "PIPELINE"
    }
}

enum class JobScope {
    ALL,
    MACHINE_TYPE,
    OWNER
}

enum class JobRecordStatus {
    RUNNING,
    FAIL,
    SUCCESS,
    UNKNOWN
}
