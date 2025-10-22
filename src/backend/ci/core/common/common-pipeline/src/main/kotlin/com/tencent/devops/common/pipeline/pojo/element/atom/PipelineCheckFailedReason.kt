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

package com.tencent.devops.common.pipeline.pojo.element.atom

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 流水线校验失败原因
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = PipelineCheckFailedMsg::class, name = PipelineCheckFailedMsg.classType),
    JsonSubTypes.Type(value = PipelineCheckFailedErrors::class, name = PipelineCheckFailedErrors.classType)
)
@Schema(title = "流水线校验失败原因")
open class PipelineCheckFailedReason(
    @get:Schema(title = "失败信息描述", required = true)
    open val message: String
)

@Schema(title = "流水线校验-简单失败原因")
data class PipelineCheckFailedMsg(
    @get:Schema(title = "失败描述信息", required = true)
    override val message: String
) : PipelineCheckFailedReason(message = message) {
    companion object {
        const val classType = "msg"
    }
}

@Schema(title = "流水线校验-多个失败原因")
data class PipelineCheckFailedErrors(
    @get:Schema(title = "失败信息描述", required = true)
    override val message: String,
    @get:Schema(title = "失败详情", required = true)
    val errors: List<ErrorInfo>
) : PipelineCheckFailedReason(message = message) {

    companion object {
        const val classType = "errors"
    }

    data class ErrorInfo(
        @get:Schema(title = "失败标题,多个插件校验时相同的错误", required = true)
        val errorTitle: String,
        @get:Schema(title = "失败详情,具体哪个插件失败详情", required = true)
        val errorDetails: Set<String>
    )
}
