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

package com.tencent.devops.environment.model

import java.time.LocalDateTime

data class CreateNodeModel(
    var nodeStringId: String? = "",
    var projectId: String,
    var nodeIp: String = "",
    var nodeName: String? = "",
    var nodeStatus: String = "",
    var nodeType: String = "",
    var nodeClusterId: String? = null,
    var nodeNamespace: String? = null,
    var createdUser: String = "",
    var expireTime: LocalDateTime? = null,
    var osName: String? = null,
    var operator: String? = null,
    var bakOperator: String? = null,
    var agentStatus: Boolean = false,
    var agentVersion: String? = null,
    var displayName: String = "",
    var image: String? = "",
    var taskId: Long? = null,
    var pipelineRefCount: Int = 0,
    var lastBuildTime: LocalDateTime? = null,
    var hostId: Long?,
    var cloudAreaId: Long?,
    var osType: String?,
    val serverId: Long?
)
