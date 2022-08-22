package com.tencent.devops.plugin.pojo.stke

// 更新configMap和workload的通用请求体
data class ConfigMapUpdateParam(
    val op: String,
    val path: String,
    val value: String
)