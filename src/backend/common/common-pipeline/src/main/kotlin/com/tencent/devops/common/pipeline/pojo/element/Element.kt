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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.pipeline.pojo.element

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitlabElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeSvnElement
import com.tencent.devops.common.pipeline.pojo.element.agent.GithubElement
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.pojo.element.agent.WindowsScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.element.service.SubPipelineCallElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.RemoteTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes(
    JsonSubTypes.Type(value = CodeGitElement::class, name = CodeGitElement.classType),
    JsonSubTypes.Type(value = CodeGitlabElement::class, name = CodeGitlabElement.classType),
    JsonSubTypes.Type(value = GithubElement::class, name = GithubElement.classType),
    JsonSubTypes.Type(value = CodeSvnElement::class, name = CodeSvnElement.classType),
    JsonSubTypes.Type(value = LinuxScriptElement::class, name = LinuxScriptElement.classType),
    JsonSubTypes.Type(value = WindowsScriptElement::class, name = WindowsScriptElement.classType),
    JsonSubTypes.Type(value = ManualReviewUserTaskElement::class, name = ManualReviewUserTaskElement.classType),
    JsonSubTypes.Type(value = ManualTriggerElement::class, name = ManualTriggerElement.classType),
    JsonSubTypes.Type(value = RemoteTriggerElement::class, name = RemoteTriggerElement.classType),
    JsonSubTypes.Type(value = TimerTriggerElement::class, name = TimerTriggerElement.classType),
    JsonSubTypes.Type(value = ManualReviewUserTaskElement::class, name = ManualReviewUserTaskElement.classType),
    JsonSubTypes.Type(value = SubPipelineCallElement::class, name = SubPipelineCallElement.classType),
    JsonSubTypes.Type(value = MarketBuildAtomElement::class, name = MarketBuildAtomElement.classType),
    JsonSubTypes.Type(value = MarketBuildLessAtomElement::class, name = MarketBuildLessAtomElement.classType)
)
abstract class Element(
    open val name: String,
    open var id: String? = null,
    open var status: String? = null,
    open var executeCount: Int = 1,
    open var canRetry: Boolean? = false,
    open var elapsed: Long? = null,
    open var startEpoch: Long? = null,
    open var version: String = "1.*",
    open var templateModify: Boolean? = null, // 模板对比的时候是不是又变更
    open var additionalOptions: ElementAdditionalOptions? = null
) {

    open fun getAtomCode() = getClassType()

    abstract fun getClassType(): String

    open fun getTaskAtom(): String = ""

    open fun genTaskParams(): MutableMap<String, Any> {
        return JsonUtil.toMutableMapSkipEmpty(this)
    }

    open fun cleanUp() {}

    open fun isElementEnable(): Boolean {
        if (additionalOptions == null) {
            return true
        }

        return additionalOptions!!.enable
    }
}
