package com.tencent.bk.codecc.task.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class GongfengBranchModel(
    val name: String,
    val protected: Boolean,
    @JsonProperty("developers_can_push")
    val developersCanPush: Boolean,
    @JsonProperty("developers_can_merge")
    val developersCanMerge: Boolean,
    val commit: BranchCommitModel
)