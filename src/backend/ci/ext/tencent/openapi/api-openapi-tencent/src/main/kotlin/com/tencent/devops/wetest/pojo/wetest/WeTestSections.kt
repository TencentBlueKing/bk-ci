package com.tencent.devops.wetest.pojo.wetest

import io.swagger.annotations.ApiModel

@ApiModel("WeTest机型筛选项信息")
data class WeTestSections(
    val ruleid: Int,
    val rulename: String,
    val sections: WeTestSection
)
