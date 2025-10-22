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

package com.tencent.devops.common.pipeline.event

import com.tencent.devops.common.pipeline.pojo.secret.ISecretParam
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "项目的流水线回调配置")
data class ProjectPipelineCallBack(
    @get:Schema(title = "流水线id", required = false)
    val id: Long? = null,
    @get:Schema(title = "项目id", required = false)
    val projectId: String,
    @get:Schema(title = "回调url地址", required = false)
    val callBackUrl: String,
    @get:Schema(title = "事件", required = false)
    val events: String,
    @get:Schema(title = "密钥", required = false)
    val secretToken: String?,
    @get:Schema(title = "回调是否启用", required = false)
    val enable: Boolean? = true,
    @get:Schema(title = "回调是否启用", required = false)
    val failureTime: LocalDateTime? = null,
    @get:Schema(title = "凭证参数", required = false)
    val secretParam: ISecretParam? = null,
    @get:Schema(title = "回调名称", required = false)
    val name: String? = null
)
