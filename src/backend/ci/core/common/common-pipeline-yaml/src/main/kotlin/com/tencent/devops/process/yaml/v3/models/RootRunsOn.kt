package com.tencent.devops.process.yaml.v3.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.pipeline.pojo.setting.RuntimeResource
import com.tencent.devops.common.pipeline.pojo.setting.RuntimeResourceType

/**
 * 流水线根级 `runs-on`，描述流水线绑定的运行资源。
 *
 * [avatarId]、[desktopId]、[poolId]、[poolName] 四个键互斥，只能填一个。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RootRunsOn(
    @JsonProperty("avatar-id")
    val avatarId: String? = null,
    @JsonProperty("desktop-id")
    val desktopId: String? = null,
    @JsonProperty("pool-name")
    val poolName: String? = null,
    @JsonProperty("pool-id")
    val poolId: String? = null,
    val workspace: String? = null
) {
    companion object {
        const val SELF = "self"

        private const val KEY_AVATAR_ID = "avatar-id"
        private const val KEY_DESKTOP_ID = "desktop-id"
        private const val KEY_POOL_ID = "pool-id"
        private const val KEY_POOL_NAME = "pool-name"
        private const val KEY_WORKSPACE = "workspace"

        fun parse(raw: Any?): RootRunsOn = when (raw) {
            // 历史写法：`runs-on: <环境名>` 或省略不写
            null -> RootRunsOn(poolName = SELF)
            is String -> RootRunsOn(poolName = raw)
            is Map<*, *> -> RootRunsOn(
                avatarId = raw[KEY_AVATAR_ID]?.toString(),
                desktopId = raw[KEY_DESKTOP_ID]?.toString(),
                poolName = raw[KEY_POOL_NAME]?.toString(),
                poolId = raw[KEY_POOL_ID]?.toString(),
                workspace = raw[KEY_WORKSPACE]?.toString()
            )

            else -> RootRunsOn(poolName = SELF)
        }

        fun fromRuntimeResource(resource: RuntimeResource?): RootRunsOn {
            if (resource == null) return RootRunsOn(poolName = SELF)
            return when (resource.type) {
                RuntimeResourceType.AVATAR -> RootRunsOn(
                    avatarId = resource.resourceId,
                    workspace = resource.workspace
                )

                RuntimeResourceType.DESKTOP -> RootRunsOn(
                    desktopId = resource.resourceId,
                    workspace = resource.workspace
                )
                // 环境资源与只绑定了环境的历史数据统一按 pool-id 输出，环境名仅在无 hashId 时兜底
                else -> RootRunsOn(
                    poolId = resource.resourceId,
                    poolName = resource.resourceName.takeIf { resource.resourceId == null },
                    workspace = resource.workspace
                )
            }
        }
    }

    /** 已填写的互斥键，用于唯一性校验 */
    fun specifiedKeys(): List<String> = buildList {
        if (!avatarId.isNullOrBlank()) add(KEY_AVATAR_ID)
        if (!desktopId.isNullOrBlank()) add(KEY_DESKTOP_ID)
        if (!poolId.isNullOrBlank()) add(KEY_POOL_ID)
        // `self` 是历史的缺省写法，不算显式指定
        if (!poolName.isNullOrBlank() && poolName != SELF) add(KEY_POOL_NAME)
    }

    /**
     * 还原为运行资源。
     *
     * 只填了 `pool-id`/`pool-name` 时无法区分数字分身环境与云桌面环境，
     * 此处保持 type 为空（与只绑定了环境的历史数据语义一致），由环境资源类型在展示层还原。
     */
    fun toRuntimeResource(): RuntimeResource? = when {
        !avatarId.isNullOrBlank() -> RuntimeResource(
            type = RuntimeResourceType.AVATAR,
            resourceId = avatarId,
            workspace = workspace
        )

        !desktopId.isNullOrBlank() -> RuntimeResource(
            type = RuntimeResourceType.DESKTOP,
            resourceId = desktopId,
            workspace = workspace
        )

        !poolId.isNullOrBlank() || (!poolName.isNullOrBlank() && poolName != SELF) -> RuntimeResource(
            type = null,
            resourceId = poolId,
            resourceName = poolName?.takeIf { it != SELF },
            workspace = workspace
        )

        else -> null
    }

    fun toYaml(): Any {
        // 只有环境名、且没有工作空间时，沿用历史的字符串写法
        if (listOf(avatarId, desktopId, poolId, workspace).all { it == null }) {
            return poolName ?: SELF
        }
        return this
    }
}
