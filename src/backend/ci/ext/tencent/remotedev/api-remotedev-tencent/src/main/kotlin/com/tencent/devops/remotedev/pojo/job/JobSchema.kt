package com.tencent.devops.remotedev.pojo.job

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

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
    val jobActionExtraParam: JobActionExtraParam
)
