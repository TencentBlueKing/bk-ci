package com.tencent.devops.wetest.pojo.wetest

import io.swagger.annotations.ApiModel

@ApiModel("WeTest机型信息")
data class WeTestModel(
    val modelid: String,
    val manu: String?,
    val model: String?,
    val version: String?,
    val resolution: String?
)
