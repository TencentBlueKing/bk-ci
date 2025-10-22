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

package com.tencent.devops.process.yaml.mq

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.process.yaml.actions.data.ActionMetaData
import com.tencent.devops.process.yaml.actions.data.EventCommonData
import com.tencent.devops.process.yaml.actions.data.PacRepoSetting
import com.tencent.devops.process.yaml.actions.data.context.PacTriggerContext

@Event(StreamBinding.PIPELINE_YAML_LISTENER_TRIGGER)
data class PipelineYamlTriggerEvent(
    override val source: String = "PacYamlTrigger",
    override val projectId: String,
    override val yamlPath: String,
    override val userId: String,
    override val eventStr: String,
    override val metaData: ActionMetaData,
    override val actionCommonData: EventCommonData,
    override val actionContext: PacTriggerContext,
    override val actionSetting: PacRepoSetting,
    val scmType: ScmType
) : BasePipelineYamlEvent(
    source = source,
    projectId = projectId,
    yamlPath = yamlPath,
    userId = userId,
    eventStr = eventStr,
    metaData = metaData,
    actionCommonData = actionCommonData,
    actionContext = actionContext,
    actionSetting = actionSetting
)
