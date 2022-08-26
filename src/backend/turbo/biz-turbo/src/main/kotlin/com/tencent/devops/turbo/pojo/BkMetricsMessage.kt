package com.tencent.devops.turbo.pojo

/**
 * turbo上报的消息内容
 *
 * @date 2022/5/26
 */
data class BkMetricsMessage(

    /**
     * 统计日期，格式yyyy-MM-dd
     */
    val statisticsTime: String,

    /**
     * 项目ID
     */
    val projectId: String,

    /**
     * 编译加速节省时间，单位：秒 保留2位小数
     */
    val turboSaveTime: Double,
)
