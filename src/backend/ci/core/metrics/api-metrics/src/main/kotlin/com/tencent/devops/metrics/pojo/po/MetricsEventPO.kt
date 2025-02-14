package com.tencent.devops.metrics.pojo.po

import com.fasterxml.jackson.annotation.JsonProperty

data class MetricsEventPO(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("data")
    val data: List<Data>,
    @JsonProperty("data_id")
    val dataId: Long
) {
    data class Data(
        @JsonProperty("dimension")
        val dimension: Map<String, String>,
        @JsonProperty("event")
        val event: Event,
        @JsonProperty("event_name")
        val eventName: String,
        @JsonProperty("target")
        val target: String = "127.0.0.1",
        @JsonProperty("timestamp")
        val timestamp: Long
    ) {

        data class Event(
            @JsonProperty("content")
            val content: String
        )
    }
}
