package com.tencent.devops.process.pojo.pipeline.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线依赖资源引用类型")
enum class PipelineDependentResourceRefType {
    ID,
    NAME
}
