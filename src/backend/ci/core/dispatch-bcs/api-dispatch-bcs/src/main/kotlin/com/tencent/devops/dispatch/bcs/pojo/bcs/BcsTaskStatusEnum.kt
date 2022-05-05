package com.tencent.devops.dispatch.bcs.pojo.bcs

enum class BcsTaskStatusEnum(val realName: String, val message: String) {
    WAITING("waiting", "任务初始化"),
    RUNNING("running", "任务正在执行"),
    FAILED("failed", "任务执行失败"),
    SUCCEEDED("succeeded", "任务执行成功"),
    UNKNOWN("unknown", "未知状态"),

    // 下面的为自定义状态，非bcs返回
    TIME_OUT("time_out", "超时");

    companion object {
        fun realNameOf(realName: String?): BcsTaskStatusEnum? {
            if (realName.isNullOrBlank()) {
                return null
            }
            return values().firstOrNull { it.realName == realName }
        }
    }
}

fun BcsTaskStatusEnum.isRunning() =
    this == BcsTaskStatusEnum.RUNNING || this == BcsTaskStatusEnum.WAITING

fun BcsTaskStatusEnum.isSuccess() = this == BcsTaskStatusEnum.SUCCEEDED

fun BcsTaskStatusEnum.isFailed() = this == BcsTaskStatusEnum.FAILED || this == BcsTaskStatusEnum.UNKNOWN
