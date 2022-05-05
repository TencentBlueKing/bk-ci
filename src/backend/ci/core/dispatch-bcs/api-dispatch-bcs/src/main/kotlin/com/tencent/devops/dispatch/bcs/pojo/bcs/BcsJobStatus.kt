package com.tencent.devops.dispatch.bcs.pojo.bcs

/**
 * Bcs job 状态信息
 * @param status job 状态
 * @param deleted true代表job已删除，false代表未被删除
 */
data class BcsJobStatus(
    val status: String,
    val deleted: Boolean
)

enum class BcsJobStatusEnum(val realName: String, val message: String) {
    PENDING("pending", "job正在创建"),
    RUNNING("running", "job正在运行"),
    FAILED("failed", "job失败"),
    SUCCEEDED("succeeded", "job成功");

    companion object {
        fun realNameOf(realName: String?): BcsTaskStatusEnum? {
            if (realName.isNullOrBlank()) {
                return null
            }
            return BcsTaskStatusEnum.values().firstOrNull { it.realName == realName }
        }
    }
}

fun BcsJobStatusEnum.isRunning() = this == BcsJobStatusEnum.RUNNING || this == BcsJobStatusEnum.PENDING

fun BcsJobStatusEnum.isSucceeded() = this == BcsJobStatusEnum.SUCCEEDED
