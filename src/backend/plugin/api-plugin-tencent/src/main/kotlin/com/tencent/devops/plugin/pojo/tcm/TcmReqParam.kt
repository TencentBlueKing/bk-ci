package com.tencent.devops.plugin.pojo.tcm

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.param.ReqParam

data class TcmReqParam(
    val operator: String,
    @field:JsonProperty("app_id")
    val appId: String,
    @field:JsonProperty("tcm_app_id")
    val tcmAppId: String,
    @field:JsonProperty("template_id")
    val templateId: String,
    val name: String,
    @field:JsonProperty("workjson")
    val workJson: List<Map<String, String>>
) : ReqParam