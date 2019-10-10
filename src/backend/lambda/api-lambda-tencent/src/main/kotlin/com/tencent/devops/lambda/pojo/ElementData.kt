package com.tencent.devops.lambda.pojo

data class ElementData(
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val elementId: String,
    val elementName: String,
    val status: String,
    val beginTime: Long,
    val endTime: Long,
    val type: String,
    val atomCode: String,
    val errorType: String? = null,
    val errorCode: Int? = null,
    val errorMsg: String? = null
)