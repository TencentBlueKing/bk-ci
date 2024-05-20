package com.tencent.devops.remotedev.pojo.startcloud

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class StartCloudResp<T>(
    val code: Int,
    val data: T?,
    val message: String?
)

/**
 * 不关注返回值的类型
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class StartCloudNoDataResp(
    val code: Int,
    val message: String?
)
