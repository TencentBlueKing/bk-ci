package com.tencent.devops.process.pojo

data class SetContextVarData(
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val contextName: String,
    val contextVal: String,
    val readOnly: Boolean?,
    val rewriteReadOnly: Boolean?
)
