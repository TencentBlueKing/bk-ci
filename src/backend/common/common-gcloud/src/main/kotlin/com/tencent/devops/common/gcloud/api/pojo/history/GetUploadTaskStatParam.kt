package com.tencent.devops.common.gcloud.api.pojo.history

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.param.ReqParam

data class GetUploadTaskStatParam(
    @field:JsonProperty("UploadTaskID")
    val uploadTaskID: String
) : ReqParam