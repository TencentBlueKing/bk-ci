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

package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.element.SendWechatNotifyElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class WechatTaskAtom @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter
) :
    IAtomTask<SendWechatNotifyElement> {
    override fun getParamElement(task: PipelineBuildTask): SendWechatNotifyElement {
        return JsonUtil.mapTo(task.taskParams, SendWechatNotifyElement::class.java)
    }

    override fun execute(
        task: PipelineBuildTask,
        param: SendWechatNotifyElement,
        runVariables: Map<String, String>
    ): AtomResponse {
        val taskId = task.taskId
        val buildId = task.buildId
        buildLogPrinter.addRedLine(
            buildId = buildId, tag = taskId, containerHashId = task.containerHashId,
            executeCount = task.executeCount ?: 1, jobId = null, stepId = task.stepId,
            message = "你可能还在懵逼中，微信告警平台已于2021年6月30日下线了，所以不要再用这个插件了，插件不报错是不希望受影响，但也收不到任何信息"
        )

        return AtomResponse(BuildStatus.SUCCEED)
    }
}
