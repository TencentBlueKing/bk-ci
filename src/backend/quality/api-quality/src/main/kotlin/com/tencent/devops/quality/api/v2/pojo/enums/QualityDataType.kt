package com.tencent.devops.quality.api.v2.pojo.enums

import io.swagger.annotations.ApiModel

@ApiModel("数据类型")
enum class QualityDataType {
    INT,
    BOOLEAN,
    FLOAT,
    STRING
}