package com.tencent.bk.codecc.task.pojo

data class TriggerPipelineModel(
        val projectId: String,
        val pipelineId: String,
        val taskId: Long,
        val gongfengId: Int,
        val owner: String
)