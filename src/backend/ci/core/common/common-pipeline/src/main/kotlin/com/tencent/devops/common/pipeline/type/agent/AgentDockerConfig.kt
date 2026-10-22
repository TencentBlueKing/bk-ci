package com.tencent.devops.common.pipeline.type.agent

/** Only explicit settings have a row. Absence means Docker and unlimited resources. */
data class AgentDockerConfig(
    val cpus: String? = null,
    val memory: String? = null,
    val containerRuntime: String = "docker"
) {
    fun applyTo(options: DockerOptions?): DockerOptions = (options ?: DockerOptions(
        volumes = null, mounts = null, gpus = null, privileged = null,
        network = null, user = null, cpus = null, memory = null
    )).copy(
        cpus = options?.cpus?.takeIf { it.isNotBlank() } ?: cpus,
        memory = options?.memory?.takeIf { it.isNotBlank() } ?: memory
    )
}
