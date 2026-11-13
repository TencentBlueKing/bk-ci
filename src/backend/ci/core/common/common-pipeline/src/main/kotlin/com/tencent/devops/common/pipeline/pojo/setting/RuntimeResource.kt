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

package com.tencent.devops.common.pipeline.pojo.setting

import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 流水线运行资源类型，四选一。
 *
 * 单体资源（[AVATAR]、[DESKTOP]）直接绑定一台机器，启动时由平台通过工作空间名换取 agentHashId；
 * 环境资源（[AVATAR_GROUP]、[DESKTOP_GROUP]）只绑定一个环境，具体在哪台机器上执行由触发方选定并随启动参数传入。
 */
enum class RuntimeResourceType(@get:JsonValue val code: String) {
    @Schema(title = "数字分身")
    AVATAR("avatar"),

    @Schema(title = "数字分身环境")
    AVATAR_GROUP("avatar-group"),

    @Schema(title = "云桌面")
    DESKTOP("desktop"),

    @Schema(title = "云桌面环境")
    DESKTOP_GROUP("desktop-group");

    /** 环境类资源：执行节点由触发方从环境下选定 */
    val group: Boolean get() = this == AVATAR_GROUP || this == DESKTOP_GROUP

    companion object {
        /** 同时兼容枚举名与 yaml/前端使用的短横线编码 */
        fun parse(code: String?): RuntimeResourceType? = code?.takeIf { it.isNotBlank() }?.let { raw ->
            entries.firstOrNull { it.code.equals(raw, true) || it.name.equals(raw, true) }
        }
    }
}

/**
 * 流水线绑定的运行资源。
 *
 * [type] 为空代表历史数据（只存了环境信息、无法区分分身或云桌面），
 * 此时按环境资源处理，运行时行为与 [RuntimeResourceType.AVATAR_GROUP] 一致。
 */
@Schema(title = "流水线运行资源")
data class RuntimeResource(
    @get:Schema(title = "运行资源类型，为空表示只绑定了环境的历史数据", required = false)
    val type: RuntimeResourceType? = null,
    @get:Schema(title = "资源标识：单体资源为工作空间名(avatar-id/desktop-id)，环境资源为环境hashId", required = false)
    val resourceId: String? = null,
    @get:Schema(title = "资源名称：环境资源为环境名称", required = false)
    val resourceName: String? = null,
    @get:Schema(title = "运行资源级工作空间", required = false)
    val workspace: String? = null
) {
    companion object {
        /**
         * 由存储字段还原运行资源：环境类资源的标识沿用 envHashId/envName，
         * 单体资源的标识存放在 runtimeResourceId。标识为空时视为未配置运行资源。
         */
        fun of(
            runtimeResourceType: RuntimeResourceType?,
            runtimeResourceId: String?,
            envHashId: String?,
            envName: String?,
            workspace: String?
        ): RuntimeResource? {
            val single = runtimeResourceType?.group == false
            val resourceId = (if (single) runtimeResourceId else envHashId)?.takeIf { it.isNotBlank() }
                ?: return null
            return RuntimeResource(
                type = runtimeResourceType,
                resourceId = resourceId,
                resourceName = envName?.takeIf { !single },
                workspace = workspace
            )
        }
    }

    /** 环境类资源（含历史数据）：执行节点由触发方从环境下选定 */
    val group: Boolean get() = type?.group ?: true

    /** 环境资源对应的环境 hashId，单体资源为空 */
    val envHashId: String? get() = resourceId?.takeIf { group }

    /** 单体资源对应的工作空间名（avatar-id / desktop-id），环境资源为空 */
    val workspaceName: String? get() = resourceId?.takeIf { !group }
}
