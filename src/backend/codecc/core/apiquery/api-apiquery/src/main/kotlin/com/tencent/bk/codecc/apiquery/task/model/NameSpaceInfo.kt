package com.tencent.bk.codecc.task.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class NameSpaceInfo(
    @JsonProperty("created_at")
    val createdAt: String,
    val description: String,
    val id: Int,
    val name: String,
    @JsonProperty("owner_id")
    val ownerId: Int,
    val path: String,
    @JsonProperty("updated_at")
    val updatedAt: String
)