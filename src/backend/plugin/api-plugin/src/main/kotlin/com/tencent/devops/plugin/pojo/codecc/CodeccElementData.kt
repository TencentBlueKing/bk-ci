package com.tencent.devops.plugin.pojo.codecc

import io.swagger.annotations.ApiModel

@ApiModel("codecc原子数据")
data class CodeccElementData(
    val projectId: String,
    val pipelineId: String,
    val taskName: String,
    val taskCnName: String,
    val taskId: String,
    val sync: String,
    val scanType: String,
    val language: String,
    val platform: String,
    val tools: String,
    val pythonVersion: String,
    val eslintRc: String,
    val codePath: String,
    val scriptType: String,
    val script: String,
    val channelCode: String,
    val updateUserId: String
)
