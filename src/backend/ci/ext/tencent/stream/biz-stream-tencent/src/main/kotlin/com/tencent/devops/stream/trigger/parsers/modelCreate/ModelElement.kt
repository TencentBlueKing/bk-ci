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

package com.tencent.devops.stream.trigger.parsers.modelCreate

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.ci.task.ServiceJobDevCloudInput
import com.tencent.devops.common.ci.task.ServiceJobDevCloudTask
import com.tencent.devops.common.ci.v2.IfType
import com.tencent.devops.common.ci.v2.Job
import com.tencent.devops.common.ci.v2.Step
import com.tencent.devops.common.ci.v2.utils.ScriptYmlUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.WindowsScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.stream.dao.GitCIServicesConfDao
import com.tencent.devops.stream.dao.GitCISettingDao
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.common.ci.v2.enums.gitEventKind.TGitObjectKind
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting
import javax.ws.rs.core.Response
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ModelElement @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val gitServicesConfDao: GitCIServicesConfDao,
    private val gitCISettingDao: GitCISettingDao
) {

    @Value("\${stream.marketRun.enable:#{false}}")
    private val marketRunTask: Boolean = false

    @Value("\${stream.marketRun.atomCode:#{null}}")
    private val runPlugInAtomCode: String? = null

    @Value("\${stream.marketRun.atomVersion:#{null}}")
    private val runPlugInVersion: String? = null

    companion object {
        private val logger = LoggerFactory.getLogger(ModelElement::class.java)
        private const val STREAM_CHECK_AUTH_TYPE = "AUTH_USER_TOKEN"
    }

    @Suppress("ComplexMethod", "NestedBlockDepth")
    fun makeElementList(
        job: Job,
        gitBasicSetting: GitCIBasicSetting,
        event: GitRequestEvent
    ): MutableList<Element> {
        // 解析service
        val elementList = makeServiceElementList(job)

        // 解析job steps
        job.steps!!.forEach { step ->
            val additionalOptions = ElementAdditionalOptions(
                continueWhenFailed = step.continueOnError ?: false,
                timeout = step.timeoutMinutes?.toLong(),
                retryWhenFailed = step.retryTimes != null,
                retryCount = step.retryTimes ?: 0,
                enableCustomEnv = step.env != null,
                customEnv = getElementEnv(step.env),
                runCondition = when {
                    step.ifFiled.isNullOrBlank() -> RunCondition.PRE_TASK_SUCCESS
                    IfType.ALWAYS_UNLESS_CANCELLED.name == (step.ifFiled ?: "") ->
                        RunCondition.PRE_TASK_FAILED_BUT_CANCEL
                    IfType.ALWAYS.name == (step.ifFiled ?: "") ->
                        RunCondition.PRE_TASK_FAILED_EVEN_CANCEL
                    IfType.FAILURE.name == (step.ifFiled ?: "") ->
                        RunCondition.PRE_TASK_FAILED_ONLY
                    else -> RunCondition.CUSTOM_CONDITION_MATCH
                },
                customCondition = step.ifFiled
            )

            // bash
            val element: Element = when {
                step.run != null -> {
                    makeRunElement(step, job, additionalOptions)
                }
                step.checkout != null -> {
                    makeCheckoutElement(step, gitBasicSetting, event).copy(additionalOptions = additionalOptions)
                }
                else -> {
                    val data = mutableMapOf<String, Any>()
                    data["input"] = step.with ?: Any()
                    MarketBuildAtomElement(
                        name = step.name ?: step.uses!!.split('@')[0],
                        id = step.id,
                        atomCode = step.uses!!.split('@')[0],
                        version = step.uses!!.split('@')[1],
                        data = data,
                        additionalOptions = additionalOptions
                    )
                }
            }

            elementList.add(element)

            if (element is MarketBuildAtomElement) {
                logger.info("install market atom: ${element.getAtomCode()}")
                ModelCommon.installMarketAtom(client, gitBasicSetting, event.userId, element.getAtomCode())
            }
        }

        return elementList
    }

    private fun makeRunElement(
        step: Step,
        job: Job,
        additionalOptions: ElementAdditionalOptions
    ): Element {
        return if (marketRunTask) {
            val data = mutableMapOf<String, Any>()
            data["input"] = mapOf("script" to step.run)
            MarketBuildAtomElement(
                name = step.name ?: "run",
                id = step.id,
                atomCode = runPlugInAtomCode ?: throw RuntimeException("runPlugInAtomCode must exist"),
                version = runPlugInVersion ?: throw RuntimeException("runPlugInVersion must exist"),
                data = data,
                additionalOptions = additionalOptions
            )
        } else {
            val linux = LinuxScriptElement(
                name = step.name ?: "run",
                id = step.id,
                scriptType = BuildScriptType.SHELL,
                script = step.run!!,
                continueNoneZero = false,
                additionalOptions = additionalOptions
            )
            if (job.runsOn.agentSelector.isNullOrEmpty()) {
                linux
            } else {
                when (job.runsOn.agentSelector!!.first()) {
                    "linux" -> linux
                    "macos" -> linux
                    "windows" -> WindowsScriptElement(
                        name = step.name ?: "run",
                        id = step.id,
                        scriptType = BuildScriptType.BAT,
                        script = step.run!!
                    )
                    else -> linux
                }
            }
        }
    }

    private fun makeCheckoutElement(
        step: Step,
        gitBasicSetting: GitCIBasicSetting,
        event: GitRequestEvent
    ): MarketBuildAtomElement {
        // checkout插件装配
        val inputMap = mutableMapOf<String, Any?>()
        if (!step.with.isNullOrEmpty()) {
            inputMap.putAll(step.with!!)
        }

        // 用户不允许指定 stream的开启人参数
        if ((inputMap["authType"] != null && inputMap["authType"] == STREAM_CHECK_AUTH_TYPE) ||
            inputMap["authUserId"] != null
        ) {
            throw CustomException(
                Response.Status.BAD_REQUEST,
                "The parameter authType:AUTH_USER_TOKEN or authUserId does not support user-specified"
            )
        }

        // 非mr和tag触发下根据commitId拉取本地工程代码
        if (step.checkout == "self") {
            inputMap["repositoryUrl"] = gitBasicSetting.gitHttpUrl.ifBlank {
                gitCISettingDao.getSetting(dslContext, gitBasicSetting.gitProjectId)?.gitHttpUrl
            }

            when (event.objectKind) {
                TGitObjectKind.MERGE_REQUEST.value ->
                    inputMap["pullType"] = "BRANCH"
                TGitObjectKind.TAG_PUSH.value -> {
                    inputMap["pullType"] = "TAG"
                    inputMap["refName"] = event.branch
                }
                TGitObjectKind.MANUAL.value -> {
                    if (event.commitId.isNotBlank()) {
                        inputMap["pullType"] = "COMMIT_ID"
                        inputMap["refName"] = event.commitId
                    } else {
                        inputMap["pullType"] = "BRANCH"
                        inputMap["refName"] = event.branch
                    }
                }
                // 定时触发根据传入的分支参数触发
                TGitObjectKind.SCHEDULE.value -> {
                    inputMap["pullType"] = "BRANCH"
                    inputMap["refName"] = event.branch
                }
                else -> {
                    inputMap["pullType"] = "COMMIT_ID"
                    inputMap["refName"] = event.commitId
                }
            }
        } else {
            inputMap["repositoryUrl"] = step.checkout!!
        }

        // 用户未指定时缺省为 AUTH_USER_TOKEN 同时指定 开启人
        if (inputMap["authType"] == null) {
            inputMap["authUserId"] = gitBasicSetting.enableUserId
            inputMap["authType"] = STREAM_CHECK_AUTH_TYPE
        }

        // 拼装插件固定参数
        inputMap["repositoryType"] = "URL"

        val data = mutableMapOf<String, Any>()
        data["input"] = inputMap

        return MarketBuildAtomElement(
            name = step.name ?: "checkout",
            id = step.id,
            atomCode = "checkout",
            version = "1.*",
            data = data
        )
    }

    private fun makeServiceElementList(job: Job): MutableList<Element> {
        val elementList = mutableListOf<Element>()

        // 解析services
        if (job.services != null) {
            job.services!!.forEach {
                val (imageName, imageTag) = ScriptYmlUtils.parseServiceImage(it.image)

                val record = gitServicesConfDao.get(dslContext, imageName, imageTag)
                    ?: throw RuntimeException("Git CI没有此镜像版本记录. ${it.image}")
                if (!record.enable) {
                    throw RuntimeException("镜像版本不可用")
                }

                val params = if (it.with.password.isNullOrBlank()) {
                    "{\"env\":{\"MYSQL_ALLOW_EMPTY_PASSWORD\":\"yes\"}}"
                } else {
                    "{\"env\":{\"MYSQL_ROOT_PASSWORD\":\"${it.with.password}\"}}"
                }

                val serviceJobDevCloudInput = ServiceJobDevCloudInput(
                    it.image,
                    record.repoUrl,
                    record.repoUsername,
                    record.repoPwd,
                    params,
                    record.env
                )
                val servicesElement = MarketBuildAtomElement(
                    name = "创建${it.image}服务",
                    id = null,
                    status = null,
                    atomCode = ServiceJobDevCloudTask.atomCode,
                    version = "1.*",
                    data = mapOf("input" to serviceJobDevCloudInput, "namespace" to (it.serviceId ?: ""))
                )

                elementList.add(servicesElement)
            }
        }

        return elementList
    }

    private fun getElementEnv(env: Map<String, Any?>?): List<NameAndValue>? {
        if (env == null) {
            return null
        }

        val nameAndValueList = mutableListOf<NameAndValue>()
        env.forEach {
            nameAndValueList.add(
                NameAndValue(
                    key = it.key,
                    value = it.value.toString()
                )
            )
        }

        return nameAndValueList
    }
}
