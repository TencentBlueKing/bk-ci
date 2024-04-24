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

@Schema(title = "OP界面创建或更新Job")
data class OpJobSchemaCreateData(
    @get:Schema(title = "自定任务Id，用户选择用")
    val jobId: String,
    @get:Schema(title = "自定任务Id，用户选择展示用")
    val jobName: String,
    @get:Schema(title = "具体JobSchema，生成前端界面用")
    val jobSchema: String,
    @get:Schema(title = "Job类型")
    val jobType: JobType,
    @get:Schema(title = "Job执行类型")
    val jobActionType: JobActionType,
    @get:Schema(title = "Job执行参数，根据执行类型不同变化")
    val jobActionParam: String
)
