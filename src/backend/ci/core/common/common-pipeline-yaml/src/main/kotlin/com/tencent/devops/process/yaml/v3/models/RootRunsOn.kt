/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
                avatarId = raw.value(KEY_AVATAR_ID),
                desktopId = raw.value(KEY_DESKTOP_ID),
                poolName = raw.value(KEY_POOL_NAME),
                poolId = raw.value(KEY_POOL_ID),
                workspace = raw.value(KEY_WORKSPACE)
            )

            else -> RootRunsOn(poolName = SELF)
        }

        private fun Map<*, *>.value(key: String): String? = this[key]?.toString()?.trim()?.takeIf { it.isNotEmpty() }

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
     * 只填了 `pool-id`/`pool-name` 时此处 type 先为空；创作流 Code 保存前会按环境记录补全类型与环境名。
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
        if (listOf(avatarId, desktopId, poolId, workspace).all { it.isNullOrBlank() }) {
            return poolName ?: SELF
        }
        return this
    }
}
