package com.tencent.devops.monitoring.constant

enum class SlaPluginError(
    private val code: String,
    private val mean: String
) {
    DEFAULT_ERROR("2199001", "插件默认异常"),
    CONFIG_ERROR("2199002", "用户配置有误"),
    DEPEND_ERROR("2199003", "插件依赖异常"),
    EXEC_FAILED("2199004", "用户任务执行失败"),
    TIMEOUT("2199005", "用户任务执行超时失败（自行限制）"),
    GITCI_ERROR("2199006", "工蜂服务异常"),
    LOW_QUALITY("2199007", "触碰质量红线"),
    ;

    companion object {
        fun getMean(code: String?): String {
            values().forEach {
                if (code == it.code) {
                    return it.mean
                }
            }
            return DEFAULT_ERROR.mean
        }
    }
}