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
 *
 */

package com.tencent.devops.process.pojo.trigger

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = PipelineTriggerFailedMatch::class, name = PipelineTriggerFailedMatch.classType),
    JsonSubTypes.Type(value = PipelineTriggerFailedErrorCode::class, name = PipelineTriggerFailedErrorCode.classType),
    JsonSubTypes.Type(value = PipelineTriggerFailedMsg::class, name = PipelineTriggerFailedMsg.classType),
    JsonSubTypes.Type(value = PipelineTriggerFailedFix::class, name = PipelineTriggerFailedFix.classType),
    JsonSubTypes.Type(
        value = PipelineTriggerDetailMessageCode::class, name = PipelineTriggerDetailMessageCode.classType
    ),
    JsonSubTypes.Type(
        value = PipelineTriggerDetailCombination::class, name = PipelineTriggerDetailCombination.classType
    )
)
@Schema(title = "流水线触发事件原因详情-基类")
@Suppress("UnnecessaryAbstractClass")
interface PipelineTriggerReasonDetail {
    @JsonIgnore
    fun getReasonDetailList(): List<String>?
}
