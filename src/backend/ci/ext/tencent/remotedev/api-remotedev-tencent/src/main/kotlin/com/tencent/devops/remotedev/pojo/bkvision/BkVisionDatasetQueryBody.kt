package com.tencent.devops.remotedev.pojo.bkvision

import com.fasterxml.jackson.annotation.JsonProperty

data class BkVisionDatasetQueryBody(
    @JsonProperty("share_uid")
    val shareUid: String,
    @JsonProperty("panel_uid")
    val panelUid: String,
    val dataset: Any,
    val query: Any,
    val option: Any
)
