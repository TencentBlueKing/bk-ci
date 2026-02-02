package com.tencent.devops.common.pipeline.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "模板实例引用类型")
enum class TemplateRefType {
    // ID引用
    ID,

    // 路径引用
    PATH
}
