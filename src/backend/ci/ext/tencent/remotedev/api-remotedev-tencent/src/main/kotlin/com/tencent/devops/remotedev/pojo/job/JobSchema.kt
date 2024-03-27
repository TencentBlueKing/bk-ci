package com.tencent.devops.remotedev.pojo.job

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class JobSchema(
    val jobSchemaId: String,
    val jobSchemaName: String,
    val schema: Map<String, Any>?,
    val jobType: JobType?
)

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
