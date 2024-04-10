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

package com.tencent.devops.common.ci.task

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tencent.devops.common.ci.CiBuildConfig
import com.tencent.devops.common.ci.TASK_TYPE
import com.tencent.devops.common.pipeline.pojo.element.Element

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = TASK_TYPE)
@JsonSubTypes(
    JsonSubTypes.Type(value = BashTask::class, name = BashTask.taskType + BashTask.taskVersion),
    JsonSubTypes.Type(value = WindowsScriptTask::class, name = WindowsScriptTask.taskType + WindowsScriptTask.taskVersion),
    JsonSubTypes.Type(value = CodeCCScanTask::class, name = CodeCCScanTask.taskType + CodeCCScanTask.taskVersion),
    JsonSubTypes.Type(value = CodeCCScanClientTask::class, name = CodeCCScanClientTask.taskType + CodeCCScanClientTask.taskVersion),
    JsonSubTypes.Type(value = DockerRunDevCloudTask::class, name = DockerRunDevCloudTask.taskType + DockerRunDevCloudTask.taskVersion),
    JsonSubTypes.Type(value = DockerBuildAndPushImageTask::class, name = DockerBuildAndPushImageTask.taskType + DockerBuildAndPushImageTask.taskVersion),
    JsonSubTypes.Type(value = MarketBuildTask::class, name = MarketBuildTask.taskType + MarketBuildTask.taskVersion),
    JsonSubTypes.Type(value = MarketBuildLessTask::class, name = MarketBuildLessTask.taskType + MarketBuildLessTask.taskVersion),
    JsonSubTypes.Type(value = CodeCCScanInContainerTask::class, name = CodeCCScanInContainerTask.taskType + CodeCCScanInContainerTask.taskVersion),
    JsonSubTypes.Type(value = SyncLocalCodeTask::class, name = SyncLocalCodeTask.taskType + SyncLocalCodeTask.taskVersion)
)

abstract class AbstractTask(
    open val displayName: String?,
    open val inputs: AbstractInput?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    open val condition: String?
) {
    abstract fun covertToElement(config: CiBuildConfig): Element
}

abstract class AbstractInput
