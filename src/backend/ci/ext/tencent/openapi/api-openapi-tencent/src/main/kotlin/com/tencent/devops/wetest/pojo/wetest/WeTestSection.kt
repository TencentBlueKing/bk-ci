package com.tencent.devops.wetest.pojo.wetest

import io.swagger.annotations.ApiModel

@ApiModel("WeTest机型筛选项信息")
data class WeTestSection(
    val id: Int,
    val rulename: String,
    val cloudid: Int,
    val rtx: String,
    val manu: String,
    val os: String,
    val resolution: String,
    val mem: String
)
