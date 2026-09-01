package com.tencent.devops.process.yaml.v3.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CreativeRunsOn(
    @JsonProperty("pool-name")
    val poolName: String? = null,
    @JsonProperty("pool-id")
    val poolId: String? = null
) {
    companion object {
        const val SELF = "self"

        fun parse(raw: Any?): CreativeRunsOn {
            return when (raw) {
                null -> CreativeRunsOn(poolName = SELF)
                is String -> CreativeRunsOn(poolName = raw)
                is Map<*, *> -> CreativeRunsOn(
                    poolName = raw["pool-name"]?.toString(),
                    poolId = raw["pool-id"]?.toString()
                )
                else -> CreativeRunsOn(poolName = SELF)
            }
        }

        fun fromSetting(envHashId: String?, envName: String?): CreativeRunsOn {
            return CreativeRunsOn(
                poolName = envName,
                poolId = envHashId
            )
        }
    }

    fun toYaml(): Any {
        if (poolId != null) {
            return this
        }
        return poolName ?: SELF
    }
}
