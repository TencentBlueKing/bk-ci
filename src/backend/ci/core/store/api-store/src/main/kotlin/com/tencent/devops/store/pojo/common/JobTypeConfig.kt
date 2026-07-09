package com.tencent.devops.store.pojo.common

import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Job 类型配置，表达 jobType 与操作系统的一对一关系。
 * 编译环境 jobType（AGENT、CREATIVE_STREAM）需要配置 osList，
 * 无编译环境 jobType（AGENT_LESS、CLOUD_TASK）不需要配置 osList。
 */
@Schema(title = "Job类型配置")
data class JobTypeConfig(
    @get:Schema(title = "Job类型", required = true)
    val jobType: JobTypeEnum,

    @get:Schema(
        title = "支持的操作系统列表。仅编译环境 jobType（如 AGENT、CREATIVE_STREAM）需要配置。" +
            "示例：[\"WINDOWS\",\"LINUX\",\"MACOS\"]",
        required = false
    )
    val osList: List<String>? = null
)
