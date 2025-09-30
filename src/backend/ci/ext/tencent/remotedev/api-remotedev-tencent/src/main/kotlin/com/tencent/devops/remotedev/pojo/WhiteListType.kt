/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.remotedev.pojo

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class WhiteList(
    val name: String,
    val type: WhiteListType,
    val creator: String,
    val windowsGpuLimit: Int? = null,
    @Schema(readOnly = true)
    val updateTime: LocalDateTime? = null
)

enum class WhiteListType {
    // 限制访问所有 remotedev api
    API,

    // 限制访问云桌面
    WINDOWS_GPU,

    // 单向网络开关-项目级别开启
    CDS_MESH_PROJECT,

    // 单向网络开关-单个实例级别开启
    CDS_MESH_WORKSPACE,

    // 单向网络开关-单个实例级别关闭-黑名单
    NOT_CDS_MESH_WORKSPACE,

    // 设备管控白名单
    PROJECT_ACCESS_DEVICE;

    companion object {
        fun parse(value: String): WhiteListType {
            return values().find { it.name == value } ?: API
        }
    }
}

@Schema(description = "提供给API使用的白名单")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ProjectAccessDeviceWhiteList::class, name = ProjectAccessDeviceWhiteList.TYPE)
)
interface IWhiteList {
    val creator: String
    val updateTime: LocalDateTime?
}

data class ProjectAccessDeviceWhiteList(
    override val creator: String,
    override val updateTime: LocalDateTime? = null,
    val projectId: String,
    val userId: String
) : IWhiteList {
    companion object {
        const val TYPE = "PROJECT_ACCESS_DEVICE"
    }
}
