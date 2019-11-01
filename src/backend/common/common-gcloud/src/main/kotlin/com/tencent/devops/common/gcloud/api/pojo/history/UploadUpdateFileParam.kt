package com.tencent.devops.common.gcloud.api.pojo.history

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.param.ReqParam

data class UploadUpdateFileParam(
    @field:JsonProperty("UploadTaskID")
    val uploadTaskId: String,
    @field:JsonProperty("TaskInfo")
    val taskInfo: String,
    @field:JsonProperty("MD5")
    val md5: String? = null,
    @field:JsonProperty("ResType")
    val resType: Int? = null,
    @field:JsonProperty("Seq")
    val seq: Int? = null,
    @field:JsonProperty("BaseVersion")
    val baseVersion: String? = null
) : ReqParam