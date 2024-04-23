package com.tencent.devops.remotedev.pojo.job

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "创建JOB需要的数据")
data class JobCreateData(
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "JOB执行范围")
    val jobScope: JobScope,
    @get:Schema(title = "JOB执行机型，范围为 MACHINE_TYPE/ALL 时")
    val machineType: String?,
    @get:Schema(title = "JOB执行拥有者，范围为 OWNER/ALL 时")
    val owners: Set<String>?,
    @get:Schema(title = "jobSchemaId")
    val jobSchemaId: String,
    @get:Schema(title = "jobSchema提交的表单数据")
    val schemaValue: Map<String, Any?>,
    @get:Schema(title = "job定时任务用户自定名称")
    val cronJobName: String?
)
