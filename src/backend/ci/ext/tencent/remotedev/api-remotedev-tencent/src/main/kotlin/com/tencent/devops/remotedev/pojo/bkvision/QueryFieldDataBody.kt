package com.tencent.devops.remotedev.pojo.bkvision

import com.fasterxml.jackson.annotation.JsonProperty

data class QueryFieldDataBody(
    @JsonProperty("share_uid")
    val shareUid: String,
    @JsonProperty("datasource_uid")
    val datasourceUid: String,
    @JsonProperty("dataset_uid")
    val datasetUid: String,
    val option: Any
)
