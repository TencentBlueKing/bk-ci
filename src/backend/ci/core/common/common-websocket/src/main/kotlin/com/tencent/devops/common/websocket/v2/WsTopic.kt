package com.tencent.devops.common.websocket.v2

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

data class WsTopic private constructor(
    val value: String,
    val kind: String,
    val projectId: String,
    val pipelineId: String,
    val buildId: String? = null,
    val executeCount: Int? = null
) {
    fun routingKeys(): Set<String> = if (kind == "pr") {
        setOf(routingKey(value), routingKey(recordScope(projectId, pipelineId, buildId!!)))
    } else {
        setOf(routingKey(value))
    }

    companion object {
        private val identifier = Regex("[A-Za-z0-9_-]{1,128}")

        fun parse(value: String): WsTopic? {
            if (value.length > 600) return null
            val parts = value.split(':')
            if (parts.size < 3 || !identifier.matches(parts[1]) || !identifier.matches(parts[2])) return null
            return when (parts[0]) {
                "ps", "ph" -> if (parts.size == 3) WsTopic(value, parts[0], parts[1], parts[2]) else null
                "pr" -> {
                    if (parts.size !in 5..6 || !identifier.matches(parts[3])) return null
                    val attempt = if (parts.size == 5 && parts[4] == "latest") {
                        null
                    } else if (parts.size == 6 && parts[4] == "attempt") {
                        parts[5].toIntOrNull()?.takeIf { it > 0 && it.toString() == parts[5] } ?: return null
                    } else {
                        return null
                    }
                    WsTopic(value, "pr", parts[1], parts[2], parts[3], attempt)
                }
                else -> null
            }
        }

        fun status(projectId: String, pipelineId: String) = "ps:$projectId:$pipelineId"
        fun history(projectId: String, pipelineId: String) = "ph:$projectId:$pipelineId"
        fun record(projectId: String, pipelineId: String, buildId: String, executeCount: Int? = null): String {
            val scope = recordScope(projectId, pipelineId, buildId)
            return if (executeCount != null && executeCount > 0) "$scope:attempt:$executeCount" else "$scope:latest"
        }

        fun recordScope(projectId: String, pipelineId: String, buildId: String) = "pr:$projectId:$pipelineId:$buildId"

        fun routingKey(topic: String): String = Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(topic.toByteArray(StandardCharsets.UTF_8))
        )
    }
}
