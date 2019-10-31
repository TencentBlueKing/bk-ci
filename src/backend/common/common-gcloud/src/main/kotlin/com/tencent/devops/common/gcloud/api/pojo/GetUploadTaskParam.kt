package com.tencent.devops.common.gcloud.api.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.param.ReqParam

data class GetUploadTaskParam(
    @field:JsonProperty("TaskID")
    val taskId: Int
) : ReqParam