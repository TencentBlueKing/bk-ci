package com.tencent.devops.dispatch.macos.pojo

data class MacRedisBuild(
    val id: String,
    val secretKey: String?,
    val gateway: String?,
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val vmSeqId: String,
    val systemVersion: String,
    val xcodeVersion: String,
    val atoms: Map<String, String> = mapOf()
)
