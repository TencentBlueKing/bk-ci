package com.tencent.devops.quality.api.v2.pojo.enums

import io.swagger.annotations.ApiModel

@ApiModel("指标类型")
enum class IndicatorType {
    SYSTEM, // op定义的指标
    CUSTOM, // 标志是用户创建的指标
    MARKET  // 原子市场指标
}
