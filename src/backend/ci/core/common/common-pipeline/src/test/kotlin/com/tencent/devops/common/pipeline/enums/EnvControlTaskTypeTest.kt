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

package com.tencent.devops.common.pipeline.enums

import com.tencent.devops.common.pipeline.pojo.element.SubPipelineCallElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitlabElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeSvnElement
import com.tencent.devops.common.pipeline.pojo.element.agent.GithubElement
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.pojo.element.agent.WindowsScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketCheckImageElement
import com.tencent.devops.common.pipeline.pojo.element.matrix.MatrixStatusElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateInElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateOutElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeP4WebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.RemoteTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EnvControlTaskTypeTest {

    @Test
    fun test() {
        assertEquals(EnvControlTaskType.parse(EnvControlTaskType.NORMAL.name), EnvControlTaskType.NORMAL)
        assertEquals(EnvControlTaskType.parse("NORMAL"), EnvControlTaskType.NORMAL)
        assertEquals(EnvControlTaskType.parse(EnvControlTaskType.VM.name), EnvControlTaskType.VM)
        assertEquals(EnvControlTaskType.parse("VM"), EnvControlTaskType.VM)

        val actualNull = null

        assertEquals(EnvControlTaskType.parse(""), actualNull)
        assertEquals(EnvControlTaskType.parse("nullActual"), actualNull)
        assertEquals(EnvControlTaskType.parse("Other"), actualNull)
        assertEquals(EnvControlTaskType.parse(CodeP4WebHookTriggerElement.classType), actualNull)
        assertEquals(EnvControlTaskType.parse(MatrixStatusElement.classType), actualNull)
        assertEquals(EnvControlTaskType.parse(CodeGitWebHookTriggerElement.classType), actualNull)
        assertEquals(EnvControlTaskType.parse(CodeGitlabWebHookTriggerElement.classType), actualNull)
        assertEquals(EnvControlTaskType.parse(CodeSVNWebHookTriggerElement.classType), actualNull)
        assertEquals(EnvControlTaskType.parse(CodeGithubWebHookTriggerElement.classType), actualNull)
        assertEquals(EnvControlTaskType.parse(CodeGitElement.classType), actualNull)
        assertEquals(EnvControlTaskType.parse(CodeGitlabElement.classType), actualNull)
        assertEquals(EnvControlTaskType.parse(GithubElement.classType), actualNull)
        assertEquals(EnvControlTaskType.parse(CodeSvnElement.classType), actualNull)
        assertEquals(EnvControlTaskType.parse(LinuxScriptElement.classType), actualNull)
        assertEquals(EnvControlTaskType.parse(WindowsScriptElement.classType), actualNull)
        assertEquals(EnvControlTaskType.parse(ManualTriggerElement.classType), actualNull)
        assertEquals(EnvControlTaskType.parse(RemoteTriggerElement.classType), actualNull)
        assertEquals(EnvControlTaskType.parse(TimerTriggerElement.classType), actualNull)
        assertEquals(EnvControlTaskType.parse(ManualReviewUserTaskElement.classType), actualNull)
        assertEquals(EnvControlTaskType.parse(SubPipelineCallElement.classType), actualNull)
        assertEquals(EnvControlTaskType.parse(MarketBuildAtomElement.classType), actualNull)
        assertEquals(EnvControlTaskType.parse(MarketBuildLessAtomElement.classType), actualNull)
        assertEquals(EnvControlTaskType.parse(MarketCheckImageElement.classType), actualNull)
        assertEquals(EnvControlTaskType.parse(QualityGateInElement.classType), actualNull)
        assertEquals(EnvControlTaskType.parse(QualityGateOutElement.classType), actualNull)
        assertEquals(EnvControlTaskType.parse(CodeTGitWebHookTriggerElement.classType), actualNull)
    }
}
