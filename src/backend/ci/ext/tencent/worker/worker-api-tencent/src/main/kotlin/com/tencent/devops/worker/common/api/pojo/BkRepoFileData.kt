package com.tencent.devops.worker.common.api.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.worker.common.api.pojo.BkRepoFile

data class BkRepoFileData(
    @JsonProperty("code")
    var code: Int,
    @JsonProperty("message")
    var message: String?,
    @JsonProperty("data")
    var data: List<BkRepoFile>
)