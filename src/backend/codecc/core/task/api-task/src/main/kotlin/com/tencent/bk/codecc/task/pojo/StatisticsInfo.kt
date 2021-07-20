package com.tencent.bk.codecc.task.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class StatisticsInfo(
    @JsonProperty("commit_count")
    val commitCount: Int,
    @JsonProperty("repository_size")
    val repositorySize: Int
)