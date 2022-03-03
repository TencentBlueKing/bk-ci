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

package com.tencent.devops.common.pipeline.pojo.element

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
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
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitGenericWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeP4WebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.RemoteTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.pipeline.utils.SkipElementUtils

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes(
    JsonSubTypes.Type(value = MatrixStatusElement::class, name = MatrixStatusElement.classType),
    JsonSubTypes.Type(value = CodeGitWebHookTriggerElement::class, name = CodeGitWebHookTriggerElement.classType),
    JsonSubTypes.Type(value = CodeGitlabWebHookTriggerElement::class, name = CodeGitlabWebHookTriggerElement.classType),
    JsonSubTypes.Type(value = CodeSVNWebHookTriggerElement::class, name = CodeSVNWebHookTriggerElement.classType),
    JsonSubTypes.Type(value = CodeGithubWebHookTriggerElement::class, name = CodeGithubWebHookTriggerElement.classType),
    JsonSubTypes.Type(value = CodeGitElement::class, name = CodeGitElement.classType),
    JsonSubTypes.Type(value = CodeGitlabElement::class, name = CodeGitlabElement.classType),
    JsonSubTypes.Type(value = GithubElement::class, name = GithubElement.classType),
    JsonSubTypes.Type(value = CodeSvnElement::class, name = CodeSvnElement.classType),
    JsonSubTypes.Type(value = LinuxScriptElement::class, name = LinuxScriptElement.classType),
    JsonSubTypes.Type(value = WindowsScriptElement::class, name = WindowsScriptElement.classType),
    JsonSubTypes.Type(value = ManualTriggerElement::class, name = ManualTriggerElement.classType),
    JsonSubTypes.Type(value = RemoteTriggerElement::class, name = RemoteTriggerElement.classType),
    JsonSubTypes.Type(value = TimerTriggerElement::class, name = TimerTriggerElement.classType),
    JsonSubTypes.Type(value = ManualReviewUserTaskElement::class, name = ManualReviewUserTaskElement.classType),
    JsonSubTypes.Type(value = SubPipelineCallElement::class, name = SubPipelineCallElement.classType),
    JsonSubTypes.Type(value = MarketBuildAtomElement::class, name = MarketBuildAtomElement.classType),
    JsonSubTypes.Type(value = MarketBuildLessAtomElement::class, name = MarketBuildLessAtomElement.classType),
    JsonSubTypes.Type(value = MarketCheckImageElement::class, name = MarketCheckImageElement.classType),
    JsonSubTypes.Type(value = QualityGateInElement::class, name = QualityGateInElement.classType),
    JsonSubTypes.Type(value = QualityGateOutElement::class, name = QualityGateOutElement.classType),
    JsonSubTypes.Type(value = CodeTGitWebHookTriggerElement::class, name = CodeTGitWebHookTriggerElement.classType),
    JsonSubTypes.Type(value = CodeGitGenericWebHookTriggerElement::class,
        name = CodeGitGenericWebHookTriggerElement.classType),
    JsonSubTypes.Type(value = CodeP4WebHookTriggerElement::class, name = CodeP4WebHookTriggerElement.classType)
)
@Suppress("ALL")
abstract class Element(
    open val name: String,
    open var id: String? = null,
    open var status: String? = null,
    open var executeCount: Int = 1,
    open var canRetry: Boolean? = null,
    open var canSkip: Boolean? = null,
    open var elapsed: Long? = null,
    open var startEpoch: Long? = null,
    open var version: String = "1.*",
    open var templateModify: Boolean? = null, // 模板对比的时候是不是又变更
    open var additionalOptions: ElementAdditionalOptions? = null,
    open var stepId: String? = null, // 用于上下文键值设置
    open var errorType: String? = null,
    open var errorCode: Int? = null,
    open var errorMsg: String? = null
) {

    open fun getAtomCode() = getClassType()

    abstract fun getClassType(): String

    open fun getTaskAtom(): String = ""

    open fun genTaskParams(): MutableMap<String, Any> {
        return JsonUtil.toMutableMap(this)
    }

    open fun cleanUp() {}

    open fun isElementEnable(): Boolean {
        if (additionalOptions == null) {
            return true
        }

        return additionalOptions!!.enable
    }

    /**
     * 根据[startType]类型返回element的id值，如果不符合，则返回空字符串""
     */
    open fun findFirstTaskIdByStartType(startType: StartType): String = ""

    /**
     * 除非是本身的[isElementEnable]设置为未启用插件会返回SKIP，或者是设置了失败手动跳过
     * [rerun]允许对状态进行重置为QUEUE
     */
    fun initStatus(rerun: Boolean = false): BuildStatus {
        return if (!isElementEnable()) { // 插件未启用
            BuildStatus.SKIP // 跳过
        } else if (rerun) { // 除以上指定跳过或不启用的以外，在final Stage 下的插件都需要重置状态
            BuildStatus.QUEUE
        } else if (status == BuildStatus.SKIP.name) { // 原本状态为SKIP，一般为 Rebuild/Fail Retry 的上一次执行标志下来
            BuildStatus.SKIP // 跳过
        } else {
            BuildStatus.QUEUE // 默认为排队状态
        }
    }

    fun disableBySkipVar(variables: Map<String, Any>) {
        val elementPostInfo = additionalOptions?.elementPostInfo
        val postFlag = elementPostInfo != null
        // post插件的父插件如果跳过执行，则其自身也需要跳过执行
        val elementId = if (postFlag) elementPostInfo?.parentElementId else id
        if (variables[SkipElementUtils.getSkipElementVariableName(elementId)] == "true") { // 参数中指明要求跳过
            if (additionalOptions == null) {
                additionalOptions = ElementAdditionalOptions(runCondition = RunCondition.PRE_TASK_SUCCESS)
            }
            additionalOptions!!.enable = false
        }
    }
}
