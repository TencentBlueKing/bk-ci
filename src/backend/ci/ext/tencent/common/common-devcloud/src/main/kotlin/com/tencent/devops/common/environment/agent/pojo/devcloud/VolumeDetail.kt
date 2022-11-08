package com.tencent.devops.common.environment.agent.pojo.devcloud

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class VolumeDetail(
    @JsonProperty("Id")
    @ApiModelProperty(name = "Id")
    val id: Int,
    val name: String,
    val volume: Int,
    val description: String,
    val defaultMountIp: String?,
    val mountTargets: List<MountTarget>
)

data class MountTarget(
    val mountTargetId: String,
    val lifeCycleState: String,
    val ip: String
)
