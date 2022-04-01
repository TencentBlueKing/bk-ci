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

import com.devops.process.yaml.modelCreate.inner.InnerModelCreator
import com.devops.process.yaml.modelCreate.inner.ModelCreateEvent
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.ci.task.ServiceJobDevCloudInput
import com.tencent.devops.common.ci.v2.Step
import com.tencent.devops.common.ci.v2.YamlTransferData
import com.tencent.devops.common.ci.v2.enums.TemplateType
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.webhook.enums.code.tgit.TGitObjectKind
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.pojo.TemplateAcrossInfoType
import com.tencent.devops.stream.dao.GitCIServicesConfDao
import com.tencent.devops.stream.dao.GitCISettingDao
import com.tencent.devops.stream.trigger.StreamTriggerCache
import com.tencent.devops.stream.v2.service.StreamOauthService
import com.tencent.devops.stream.v2.service.StreamScmService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.ws.rs.core.Response

@Component
class InnerModelCreatorImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val gitServicesConfDao: GitCIServicesConfDao,
    private val gitCISettingDao: GitCISettingDao,
    private val streamTriggerCache: StreamTriggerCache,
    private val streamScmService: StreamScmService,
    private val oauthService: StreamOauthService
) : InnerModelCreator {

    @Value("\${stream.marketRun.enable:#{false}}")
    private val marketRunTaskData: Boolean = false

    @Value("\${stream.marketRun.atomCode:#{null}}")
    private val runPlugInAtomCodeData: String? = null

    @Value("\${stream.marketRun.atomVersion:#{null}}")
    private val runPlugInVersionData: String? = null

    @Value("\${container.defaultImage:#{null}}")
    private val defaultImageData: String = "http://mirrors.tencent.com/ci/tlinux3_ci:1.5.0"

    companion object {
        private const val STREAM_CHECK_AUTH_TYPE = "AUTH_USER_TOKEN"
    }

    override val marketRunTask: Boolean
        get() = marketRunTaskData

    override val runPlugInAtomCode: String?
        get() = runPlugInAtomCodeData

    override val runPlugInVersion: String?
        get() = runPlugInVersionData

    override val defaultImage: String
        get() = defaultImageData

    override fun getJobTemplateAcrossInfo(
        yamlTransferData: YamlTransferData,
        gitRequestEventId: Long,
        gitProjectId: Long
    ): Map<String, BuildTemplateAcrossInfo> {
        // 临时保存远程项目id的映射，就不用去redis里面查了
        val remoteProjectIdMap = mutableMapOf<String, String>()
        yamlTransferData.templateData.transferDataMap.values.forEach { objectData ->
            if (objectData.remoteProjectId in remoteProjectIdMap.keys) {
                return@forEach
            }
            // 将pathWithPathSpace转为数字id
            remoteProjectIdMap[objectData.remoteProjectId] = streamTriggerCache.getAndSaveRequestGitProjectInfo(
                gitRequestEventId = gitRequestEventId,
                gitProjectId = objectData.remoteProjectId,
                token = oauthService.getGitCIEnableToken(gitProjectId).accessToken,
                useAccessToken = true,
                getProjectInfo = streamScmService::getProjectInfoRetry
            ).gitProjectId.toString().let { "git_$it" }
        }

        val results = mutableMapOf<String, BuildTemplateAcrossInfo>()
        yamlTransferData.templateData.transferDataMap.filter { it.value.templateType == TemplateType.JOB }
            .forEach { (objectId, objectData) ->
                results[objectId] = BuildTemplateAcrossInfo(
                    templateId = yamlTransferData.templateData.templateId,
                    templateType = TemplateAcrossInfoType.JOB,
                    // 因为已经将jobId转为了map所以这里不保存，节省空间
                    templateInstancesIds = emptyList(),
                    targetProjectId = remoteProjectIdMap[objectData.remoteProjectId]!!
                )
            }
        return results
    }

    override fun getServiceJobDevCloudInput(
        image: String,
        imageName: String,
        imageTag: String,
        params: String
    ): ServiceJobDevCloudInput {
        val record = gitServicesConfDao.get(dslContext, imageName, imageTag)
            ?: throw RuntimeException("Git CI没有此镜像版本记录. $image")
        if (!record.enable) {
            throw RuntimeException("镜像版本不可用")
        }

        return ServiceJobDevCloudInput(
            image = image,
            registryHost = record.repoUrl,
            registryUsername = record.repoUsername,
            registryPassword = record.repoPwd,
            params = params,
            serviceEnv = record.env
        )
    }

    override fun makeCheckoutElement(
        step: Step,
        event: ModelCreateEvent,
        additionalOptions: ElementAdditionalOptions
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
            makeCheckoutSelf(inputMap, event)
        } else {
            inputMap["repositoryUrl"] = step.checkout!!
        }

        // 用户未指定时缺省为 AUTH_USER_TOKEN 同时指定 开启人
        if (inputMap["authType"] == null) {
            inputMap["authUserId"] = event.streamData?.enableUserId
            inputMap["authType"] = STREAM_CHECK_AUTH_TYPE
        }

        // 拼装插件固定参数
        inputMap["repositoryType"] = "URL"

        val data = mutableMapOf<String, Any>()
        data["input"] = inputMap

        return MarketBuildAtomElement(
            id = step.taskId,
            name = step.name ?: "checkout",
            stepId = step.id,
            atomCode = "checkout",
            version = "1.*",
            data = data,
            additionalOptions = additionalOptions
        )
    }

    private fun makeCheckoutSelf(inputMap: MutableMap<String, Any?>, event: ModelCreateEvent) {
        val gitData = event.gitData!!
        inputMap["repositoryUrl"] = gitData.repositoryUrl.ifBlank {
            retryGetRepositoryUrl(gitData.gitProjectId.toString())
        }

        when (event.streamData!!.objectKind) {
            TGitObjectKind.MERGE_REQUEST -> {
                inputMap["pullType"] = "BRANCH"
                // mr merged时,需要拉取目标分支,如果是mr open,插件不会读取这个值
                inputMap["refName"] = gitData.branch
            }
            TGitObjectKind.TAG_PUSH -> {
                inputMap["pullType"] = "TAG"
                inputMap["refName"] = gitData.branch
            }
            TGitObjectKind.PUSH -> {
                inputMap["pullType"] = "BRANCH"
                inputMap["refName"] = gitData.branch
            }
            TGitObjectKind.MANUAL -> {
                if (gitData.commitId.isNotBlank()) {
                    inputMap["pullType"] = "COMMIT_ID"
                    inputMap["refName"] = gitData.commitId
                } else {
                    inputMap["pullType"] = "BRANCH"
                    inputMap["refName"] = gitData.branch
                }
            }
            // 定时触发根据传入的分支参数触发
            TGitObjectKind.SCHEDULE -> {
                inputMap["pullType"] = "BRANCH"
                inputMap["refName"] = gitData.branch
            }
            else -> {
                inputMap["pullType"] = "COMMIT_ID"
                inputMap["refName"] = gitData.commitId
            }
        }
    }

    private fun retryGetRepositoryUrl(projectId: String): String? {
        return gitCISettingDao.getSetting(dslContext, projectId.toLong())?.gitHttpUrl
    }
}
