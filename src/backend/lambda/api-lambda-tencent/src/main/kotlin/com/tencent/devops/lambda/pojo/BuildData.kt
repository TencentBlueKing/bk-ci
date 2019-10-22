package com.tencent.devops.lambda.pojo

data class BuildData(
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val userId: String,
    val status: String,
    val trigger: String,
    val beginTime: Long,
    val endTime: Long,
    val buildNum: Int,
    val templateId: String,
    val bgName: String,
    val deptName: String,
    val centerName: String,
    val model: String,
    val errorType: String? = null,
    val errorCode: Int? = null,
    val errorMsg: String? = null
)