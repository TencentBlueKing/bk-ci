package com.tencent.devops.dispatch.codecc.pojo.devcloud

import com.fasterxml.jackson.annotation.JsonValue

data class DevCloudContainerOperation(
    val action: String,
    val params: ContainerParams?,
    val command: List<String>?
)

data class ContainerParams(
    val image: String?,
    val registry: Registry?,
    val cpu: Int?,
    val memory: String?,
    val replica: Int?
)

enum class Action(private val action: String) {
    START("start"),
    STOP("start"),
    RECREATE("recreate"),
    SCALE("scale"),
    DELETE("delete");

    @JsonValue
    fun getValue(): String {
        return action
    }
}
