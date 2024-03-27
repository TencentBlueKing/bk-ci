package com.tencent.devops.remotedev.pojo.job

import io.swagger.v3.oas.annotations.media.Schema

open class JobSchema(
    override val jobSchemaId: String,
    override val jobSchemaName: String,
    open val schema: Map<String, Any>,
    open val jobType: JobType
) : JobSchemaShort(jobSchemaId = jobSchemaId, jobSchemaName = jobSchemaName)

// 列表中的简短展示
open class JobSchemaShort(
    open val jobSchemaId: String,
    open val jobSchemaName: String
)

data class JobSchemaWithExtra(
    override val jobSchemaId: String,
    override val jobSchemaName: String,
    override val schema: Map<String, Any>,
    override val jobType: JobType,
    val jobActionType: JobActionType,
    val jobActionExtraParam: JobActionExtraParam?
) : JobSchema(jobSchemaId = jobSchemaId, jobSchemaName = jobSchemaName, schema = schema, jobType = jobType)

data class JobSchemaCreateData(
    val jobId: String,
    val jobName: String,
    val jobSchema: Map<String, Any>,
    val jobType: JobType,
    val jobActionType: JobActionType,
    @get:Schema(title = "根据jobActionType选择的不同传递不同的参数")
    val jobNotifyRemoteDevDesktopActionExtraParam: JobBackendActionExtraParam?,
    val jobNotifyCronPowerOnActionExtraParam: JobBackendActionExtraParam?,
    val jobPipelineActionExtraParam: JobPipelineActionExtraParam?
)
