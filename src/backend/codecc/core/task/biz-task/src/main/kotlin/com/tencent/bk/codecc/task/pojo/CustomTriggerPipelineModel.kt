package com.tencent.bk.codecc.task.pojo

import com.tencent.bk.codecc.task.model.CustomProjEntity

data class CustomTriggerPipelineModel(
    val customProjEntity: CustomProjEntity,
    val runtimeParam: Map<String, String>,
    val userId: String
)