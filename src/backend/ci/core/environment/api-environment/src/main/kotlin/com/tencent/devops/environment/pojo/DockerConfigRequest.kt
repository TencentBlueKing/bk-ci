package com.tencent.devops.environment.pojo

import com.tencent.devops.common.api.exception.InvalidParamException
import java.math.BigDecimal

enum class DockerConfigScope { ENV, NODE }

/** null: keep, blank: unlimited. In particular, a batch never clears an omitted field. */
data class DockerConfigRequest(
    val cpus: String? = null,
    val memory: String? = null,
    val containerRuntime: String? = null
) {
    fun validate() {
        if (!cpus.isNullOrEmpty() && (cpus.length > 64 || !cpus.matches(Regex("[0-9]+(\\.[0-9]+)?")) ||
                cpus.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } != true)) {
            throw InvalidParamException("CPU 上限须为大于 0 的数，支持小数")
        }
        if (!memory.isNullOrEmpty()) {
            if (memory.length > 64 || !memory.matches(Regex("[1-9][0-9]*[mg]"))) {
                throw InvalidParamException("内存上限支持 m、g，例如 512m、4g")
            }
            if (memory.endsWith("m") && memory.dropLast(1).toBigDecimal() < BigDecimal(6)) {
                throw InvalidParamException("内存上限不能低于 6m")
            }
        }
        if (containerRuntime != null && containerRuntime !in setOf("docker", "podman")) {
            throw InvalidParamException("Container runtime must be docker or podman")
        }
    }
}

data class BatchDockerConfigRequest(val nodeIds: Set<String>, val config: DockerConfigRequest)
