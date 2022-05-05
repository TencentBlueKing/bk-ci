package com.tencent.devops.dispatch.bcs.pojo.bcs

import com.fasterxml.jackson.annotation.JsonProperty

interface BcsOperateBuilderParams

class BcsDeleteBuilderParams(
    // TODO: BCS端有点问题，先写死
    @JsonProperty("ProjectID")
    val projectId: String = "landun"
) : BcsOperateBuilderParams

class BcsStopBuilderParams : BcsOperateBuilderParams

data class BcsStartBuilderParams(
    val env: Map<String, String>?,
    val command: List<String>?
) : BcsOperateBuilderParams
