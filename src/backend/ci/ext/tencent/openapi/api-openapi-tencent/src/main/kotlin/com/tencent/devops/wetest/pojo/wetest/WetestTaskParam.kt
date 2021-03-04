package com.tencent.devops.wetest.pojo.wetest

data class WetestTaskParam(
    val name: String,
    val mobileCategory: String,
    val mobileCategoryId: String,
    val mobileModel: String,
    val mobileModelId: String,
    val deviceType: String,
    val description: String?
)
