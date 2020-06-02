package com.tencent.bk.codecc.quartz.pojo

data class JobExternalDto(
        val jobName: String?,
        val classUrl: String,
        val className: String,
        val cronExpression: String,
        val jobCustomParam: Map<String, Any>,
        val operType: OperationType
)