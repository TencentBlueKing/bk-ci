package com.tencent.devops.quality.api.v2.pojo.enums

import io.swagger.annotations.ApiModel

@ApiModel("模板类型")
enum class TemplateType {
    TEMPLATE, // 模板
    INDICATOR_SET // 指标集
}