package com.tencent.bk.codecc.task.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class ProjectCommitInfo(
    @JsonProperty("username")
    val userName: String,
    @JsonProperty("total_work")
    val totalWork: Int,
    @JsonProperty("total_add")
    val totalAdd: Int,
    @JsonProperty("total_mod")
    val totalMod: Int,
    @JsonProperty("total_del")
    val totalDel: Int,
    @JsonProperty("source_add")
    val sourceAdd: Int,
    @JsonProperty("source_mod")
    val sourceMod: Int,
    @JsonProperty("source_del")
    val sourceDel: Int,
    @JsonProperty("comment_add")
    val commentAdd: Int,
    @JsonProperty("comment_mod")
    val commentMod: Int,
    @JsonProperty("comment_del")
    val commentDel: Int,
    @JsonProperty("blank_add")
    val blankAdd: Int,
    @JsonProperty("blank_mod")
    val blankMod: Int,
    @JsonProperty("blank_del")
    val blankDel: Int
)