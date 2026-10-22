package com.tencent.devops.environment.service.thirdpartyagent

import com.fasterxml.jackson.databind.JsonNode
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.environment.pojo.AgentResourceSnapshot

/** Never derive available memory from free memory, or select the largest disk. */
object AgentResourceParser {
    fun parse(data: String, workDir: String?, now: Long = System.currentTimeMillis()): AgentResourceSnapshot {
        val root = JsonUtil.getObjectMapper().readTree(data)
        val metrics = if (root.has("metrics")) root.path("metrics").toList() else listOf(root)
        val valid = metrics.filter {
            val seconds = it.path("timestamp").takeIf {
                value -> value.isIntegralNumber && value.canConvertToLong()
            }?.asLong()
            seconds != null && seconds > 0 && seconds <= now / 1000 &&
                now - seconds * 1000 <= AgentResourceSnapshot.MAX_AGE_MILLIS
        }
        val cpu = valid.filter {
            it.path("name").asText() in setOf("cpu", "cpu_detail", "win_cpu") &&
                (it.path("tags").path("cpu").asText() == "cpu-total" ||
                    it.path("tags").path("instance").asText() in setOf("_Total", "cpu-total"))
        }.maxByOrNull { it.path("timestamp").asLong() }
        val memory = valid.filter { it.path("name").asText() == "mem" }
            .maxByOrNull { it.path("timestamp").asLong() }
        val path = workDir?.replace('\\', '/')?.let { if (it == "/") it else it.trimEnd('/') }
        val disk = if (path.isNullOrBlank()) null else valid.filter {
            if (it.path("name").asText() != "disk") return@filter false
            val mount = it.path("tags").path("path").asText().replace('\\', '/').trimEnd('/')
            val ignoreCase = path.length > 1 && path[1] == ':'
            (mount.isNotBlank() || it.path("tags").path("path").asText() == "/") &&
                (path.equals(mount, ignoreCase) || path.startsWith("$mount/", ignoreCase))
        }.maxWithOrNull(compareBy<JsonNode> { it.path("tags").path("path").asText().length }
            .thenBy { it.path("timestamp").asLong() })
        fun number(metric: JsonNode?, vararg keys: String): Double? = keys.firstNotNullOfOrNull { key ->
            metric?.path("fields")?.get(key)?.takeIf(JsonNode::isNumber)?.asDouble()?.takeIf(Double::isFinite)
        }
        fun ratio(metric: JsonNode?, key: String): Double? {
            val total = number(metric, "total")?.takeIf { it > 0 } ?: return null
            return number(metric, key)?.let { 100 * it / total }
        }
        fun percentage(value: Double?) = value?.takeIf { it.isFinite() && it in 0.0..100.0 }
        val cpuValue = percentage(number(cpu, "idle", "usage_idle", "Percent_Idle_Time"))
        val memoryValue = percentage(number(memory, "available_percent") ?: ratio(memory, "available"))
        val diskValue = percentage(ratio(disk, "free"))
        fun time(metric: JsonNode?, value: Double?) = value?.let { metric?.path("timestamp")?.asLong()?.times(1000) }
        return AgentResourceSnapshot(
            cpuValue, time(cpu, cpuValue), memoryValue, time(memory, memoryValue), diskValue, time(disk, diskValue)
        )
    }
}
