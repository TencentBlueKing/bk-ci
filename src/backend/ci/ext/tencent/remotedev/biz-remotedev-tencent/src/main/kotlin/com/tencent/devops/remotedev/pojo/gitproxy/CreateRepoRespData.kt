package com.tencent.devops.remotedev.pojo.gitproxy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateRepoRespData(
    val configuration: CreateRepoRespDataConf?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateRepoRespDataConf(
    val settings: CreateRepoRespDataConfSettings?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateRepoRespDataConfSettings(
    val clientUrl: String?
)