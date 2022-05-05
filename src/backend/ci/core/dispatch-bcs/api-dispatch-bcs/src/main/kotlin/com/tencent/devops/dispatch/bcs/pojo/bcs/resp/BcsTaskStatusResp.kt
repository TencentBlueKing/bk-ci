package com.tencent.devops.dispatch.bcs.pojo.bcs.resp

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * bcs任务状态
 * @param status 任务状态
 * @param message 请求成功但是结果失败时的错误信息
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class BcsTaskStatusResp(
    val status: String,
    val message: String?
)
