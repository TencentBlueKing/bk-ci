package com.tencent.devops.dispatch.bcs.pojo.bcs.resp

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 创建task类的接口返回
 * @param taskId 任务id
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class BcsTaskResp(
    @JsonProperty("task_id")
    val taskId: String
)
