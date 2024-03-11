package com.tencent.devops.common.auth.callback

import com.tencent.bk.sdk.iam.dto.callback.response.SchemaProperties
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "资源类型schema定义")
data class FetchResourceTypeSchemaProperties(
    val projectId: SchemaProperties,
    val projectName: SchemaProperties,
    val pipelineId: SchemaProperties,
    val pipelineName: SchemaProperties
)
