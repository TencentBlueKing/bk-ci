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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_BASE_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_COMMIT_MESSAGE
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT_CONTENT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_HEAD_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_GROUP
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_NAME
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA_SHORT
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.process.utils.PIPELINE_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PROJECT_NAME
import com.tencent.devops.ticket.api.ServiceCredentialResource
import com.tencent.devops.ticket.pojo.Credential
import com.tencent.devops.ticket.pojo.enums.CredentialType
import com.tencent.devops.ticket.pojo.enums.Permission
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Base64

@Suppress("ALL")
@Service
class PipelineContextService@Autowired constructor(
    private val pipelineBuildDetailService: PipelineBuildDetailService,
    private val client: Client
) {
    private val logger = LoggerFactory.getLogger(PipelineContextService::class.java)

    fun buildContext(buildId: String, containerId: String?, buildVar: Map<String, String>): Map<String, String> {
        val modelDetail = pipelineBuildDetailService.get(buildId) ?: return emptyMap()
        val varMap = mutableMapOf<String, String>()
        try {
            modelDetail.model.stages.forEach { stage ->
                stage.containers.forEach { c ->
                    buildJobContext(c, containerId, varMap, stage)
                    // steps
                    buildStepContext(c, varMap, buildVar)
                }
            }
            buildCiContext(varMap, modelDetail, buildVar)
            buildCredentialContext(buildVar, varMap)
        } catch (e: Throwable) {
            logger.error("Build context failed,", e)
        }

        return varMap
    }

    private fun buildCredentialContext(
        buildVar: Map<String, String>,
        varMap: MutableMap<String, String>
    ) {
        val userId = buildVar[PIPELINE_START_USER_ID]
        val projectId = buildVar[PROJECT_NAME]
        if (!userId.isNullOrBlank() && !projectId.isNullOrBlank()) {
            val credentials = client.get(ServiceCredentialResource::class).hasPermissionList(
                userId = userId,
                projectId = projectId,
                credentialTypesString = null,
                permission = Permission.USE,
                page = null,
                pageSize = null,
                keyword = null
            ).data
            if (credentials != null && credentials.records.isNotEmpty()) {
                credentials.records.forEach { credential ->
                    varMap["settings.${credential.credentialId}"] = credential.credentialId
                    varMap.putAll(getKeyMap(projectId, credential))
                }
            }
        }
    }

    private fun getKeyMap(projectId: String, credential: Credential): Map<String, String> {
        val credentialMap = getCredential(projectId, credential.credentialId)

        val keyMap = mutableMapOf<String, String>()
        when (credential.credentialType) {
            CredentialType.PASSWORD -> {
                if (credentialMap["v1"] != null) {
                    keyMap["settings.${credential.credentialId}.password"] = credentialMap["v1"]!!
                }
            }
            CredentialType.ACCESSTOKEN -> {
                if (credentialMap["v1"] != null) {
                    keyMap["settings.${credential.credentialId}.access_token"] = credentialMap["v1"]!!
                }
            }
            CredentialType.USERNAME_PASSWORD -> {
                if (credentialMap["v1"] != null) {
                    keyMap["settings.${credential.credentialId}.username"] = credentialMap["v1"]!!
                }
                if (credentialMap["v2"] != null) {
                    keyMap["settings.${credential.credentialId}.password"] = credentialMap["v2"]!!
                }
            }
            CredentialType.SECRETKEY -> {
                if (credentialMap["v1"] != null) {
                    keyMap["settings.${credential.credentialId}.secretKey"] = credentialMap["v1"]!!
                }
            }
            CredentialType.APPID_SECRETKEY -> {
                if (credentialMap["v1"] != null) {
                    keyMap["settings.${credential.credentialId}.appId"] = credentialMap["v1"]!!
                }
                if (credentialMap["v2"] != null) {
                    keyMap["settings.${credential.credentialId}.secretKey"] = credentialMap["v2"]!!
                }
            }
            CredentialType.SSH_PRIVATEKEY -> {
                if (credentialMap["v1"] != null) {
                    keyMap["settings.${credential.credentialId}.privateKey"] = credentialMap["v1"]!!
                }
                if (credentialMap["v2"] != null) {
                    keyMap["settings.${credential.credentialId}.passphrase"] = credentialMap["v2"]!!
                }
            }
            CredentialType.TOKEN_SSH_PRIVATEKEY -> {
                if (credentialMap["v1"] != null) {
                    keyMap["settings.${credential.credentialId}.token"] = credentialMap["v1"]!!
                }
                if (credentialMap["v2"] != null) {
                    keyMap["settings.${credential.credentialId}.privateKey"] = credentialMap["v2"]!!
                }
                if (credentialMap["v3"] != null) {
                    keyMap["settings.${credential.credentialId}.passphrase"] = credentialMap["v3"]!!
                }
            }
            CredentialType.TOKEN_USERNAME_PASSWORD -> {
                if (credentialMap["v1"] != null) {
                    keyMap["settings.${credential.credentialId}.token"] = credentialMap["v1"]!!
                }
                if (credentialMap["v2"] != null) {
                    keyMap["settings.${credential.credentialId}.username"] = credentialMap["v2"]!!
                }
                if (credentialMap["v3"] != null) {
                    keyMap["settings.${credential.credentialId}.password"] = credentialMap["v3"]!!
                }
            }
            CredentialType.COS_APPID_SECRETID_SECRETKEY_REGION -> {
                if (credentialMap["v1"] != null) {
                    keyMap["settings.${credential.credentialId}.cosappId"] = credentialMap["v1"]!!
                }
                if (credentialMap["v2"] != null) {
                    keyMap["settings.${credential.credentialId}.secretId"] = credentialMap["v2"]!!
                }
                if (credentialMap["v3"] != null) {
                    keyMap["settings.${credential.credentialId}.secretKey"] = credentialMap["v3"]!!
                }
                if (credentialMap["v4"] != null) {
                    keyMap["settings.${credential.credentialId}.region"] = credentialMap["v4"]!!
                }
            }
            CredentialType.MULTI_LINE_PASSWORD -> {
                if (credentialMap["v1"] != null) {
                    keyMap["settings.${credential.credentialId}.password"] = credentialMap["v1"]!!
                }
            }
        }
        return keyMap
    }

    fun getCredential(
        projectId: String,
        credentialId: String
    ): MutableMap<String, String> {
        val pair = DHUtil.initKey()
        val encoder = Base64.getEncoder()
        val decoder = Base64.getDecoder()
        val credentialInfo = client.get(ServiceCredentialResource::class).get(projectId, credentialId,
            encoder.encodeToString(pair.publicKey)).data ?: return mutableMapOf()

        val ticketMap = mutableMapOf<String, String>()
        val v1 = String(DHUtil.decrypt(
            decoder.decode(credentialInfo.v1),
            decoder.decode(credentialInfo.publicKey),
            pair.privateKey))
        ticketMap["v1"] = v1

        if (credentialInfo.v2 != null && credentialInfo.v2!!.isNotEmpty()) {
            val v2 = String(DHUtil.decrypt(
                decoder.decode(credentialInfo.v2),
                decoder.decode(credentialInfo.publicKey),
                pair.privateKey))
            ticketMap["v2"] = v2
        }

        if (credentialInfo.v3 != null && credentialInfo.v3!!.isNotEmpty()) {
            val v3 = String(DHUtil.decrypt(
                decoder.decode(credentialInfo.v3),
                decoder.decode(credentialInfo.publicKey),
                pair.privateKey))
            ticketMap["v3"] = v3
        }

        if (credentialInfo.v4 != null && credentialInfo.v4!!.isNotEmpty()) {
            val v4 = String(DHUtil.decrypt(
                decoder.decode(credentialInfo.v4),
                decoder.decode(credentialInfo.publicKey),
                pair.privateKey))
            ticketMap["v4"] = v4
        }

        return ticketMap
    }

    private fun buildCiContext(
        varMap: MutableMap<String, String>,
        modelDetail: ModelDetail,
        buildVar: Map<String, String>
    ) {
        varMap["ci.pipeline_name"] = modelDetail.pipelineName
        varMap["ci.build_id"] = buildVar[PIPELINE_BUILD_ID] ?: ""
        varMap["ci.build_num"] = buildVar[PIPELINE_BUILD_NUM] ?: ""
        varMap["ci.pipeline_name"] = modelDetail.pipelineName
        varMap["ci.actor"] = modelDetail.userId
        if (!buildVar[PIPELINE_GIT_REF].isNullOrBlank())
            varMap[PIPELINE_GIT_REF] = buildVar[PIPELINE_GIT_REF]!!
        if (!buildVar[PIPELINE_GIT_HEAD_REF].isNullOrBlank())
            varMap[PIPELINE_GIT_HEAD_REF] = buildVar[PIPELINE_GIT_HEAD_REF]!!
        if (!buildVar[PIPELINE_GIT_BASE_REF].isNullOrBlank())
            varMap[PIPELINE_GIT_BASE_REF] = buildVar[PIPELINE_GIT_BASE_REF]!!
        if (!buildVar[PIPELINE_GIT_REPO].isNullOrBlank())
            varMap[PIPELINE_GIT_REPO] = buildVar[PIPELINE_GIT_REPO]!!
        if (!buildVar[PIPELINE_GIT_REPO_NAME].isNullOrBlank())
            varMap[PIPELINE_GIT_REPO_NAME] = buildVar[PIPELINE_GIT_REPO_NAME]!!
        if (!buildVar[PIPELINE_GIT_REPO_GROUP].isNullOrBlank())
            varMap[PIPELINE_GIT_REPO_GROUP] = buildVar[PIPELINE_GIT_REPO_GROUP]!!
        if (!buildVar[PIPELINE_GIT_EVENT].isNullOrBlank())
            varMap[PIPELINE_GIT_EVENT] = buildVar[PIPELINE_GIT_EVENT]!!
        if (!buildVar[PIPELINE_GIT_EVENT_CONTENT].isNullOrBlank())
            varMap[PIPELINE_GIT_EVENT_CONTENT] = buildVar[PIPELINE_GIT_EVENT_CONTENT]!!
        if (!buildVar[PIPELINE_GIT_SHA].isNullOrBlank())
            varMap[PIPELINE_GIT_SHA] = buildVar[PIPELINE_GIT_SHA]!!
        if (!buildVar[PIPELINE_GIT_SHA_SHORT].isNullOrBlank())
            varMap[PIPELINE_GIT_SHA_SHORT] = buildVar[PIPELINE_GIT_SHA_SHORT]!!
        if (!buildVar[PIPELINE_GIT_COMMIT_MESSAGE].isNullOrBlank())
            varMap[PIPELINE_GIT_COMMIT_MESSAGE] = buildVar[PIPELINE_GIT_COMMIT_MESSAGE]!!
    }

    private fun buildStepContext(
        c: Container,
        varMap: MutableMap<String, String>,
        buildVar: Map<String, String>
    ) {
        c.elements.forEach { e ->
            varMap["jobs.${c.jobId ?: ""}.steps.${e.id}.name"] = e.name
            varMap["jobs.${c.jobId ?: ""}.steps.${e.id}.id"] = e.id ?: ""
            varMap["jobs.${c.jobId ?: ""}.steps.${e.id}.status"] = getStepStatus(e)
            varMap["jobs.${c.jobId ?: ""}.steps.${e.id}.outcome"] = e.status ?: ""
            varMap["steps.${e.id}.name"] = e.name
            varMap["steps.${e.id}.id"] = e.id ?: ""
            varMap["steps.${e.id}.status"] = getStepStatus(e)
            varMap["steps.${e.id}.outcome"] = e.status ?: ""
            varMap.putAll(getStepOutput(c, e, buildVar))
        }
    }

    private fun buildJobContext(
        c: Container,
        containerId: String?,
        varMap: MutableMap<String, String>,
        stage: Stage
    ) {
        // current job
        if (c.id != null && c.id!! == containerId) {
            varMap["job.id"] = c.jobId ?: ""
            varMap["job.name"] = c.name
            varMap["job.status"] = getJobStatus(c)
            varMap["job.outcome"] = c.status ?: ""
            varMap["job.os"] = getOs(c)
            varMap["job.container.network"] = getNetWork(c)
            varMap["job.stage_id"] = stage.id ?: ""
            varMap["job.stage_name"] = stage.name ?: ""
        }

        // other job
        varMap["jobs.${c.jobId ?: c.id ?: ""}.id"] = c.jobId ?: ""
        varMap["jobs.${c.jobId ?: c.id ?: ""}.name"] = c.name
        varMap["jobs.${c.jobId ?: c.id ?: ""}.status"] = getJobStatus(c)
        varMap["jobs.${c.jobId ?: c.id ?: ""}.outcome"] = c.status ?: ""
        varMap["jobs.${c.jobId ?: c.id ?: ""}.os"] = getOs(c)
        varMap["jobs.${c.jobId ?: c.id ?: ""}.container.network"] = getNetWork(c)
        varMap["jobs.${c.jobId ?: c.id ?: ""}.stage_id"] = stage.id ?: ""
        varMap["jobs.${c.jobId ?: c.id ?: ""}.stage_name"] = stage.name ?: ""
    }

    private fun getStepOutput(c: Container, e: Element, buildVar: Map<String, String>): Map<out String, String> {
        val outputMap = mutableMapOf<String, String>()
        buildVar.filterKeys { it.startsWith("steps.${e.id ?: ""}.outputs.") }.forEach { (t, u) ->
            outputMap["jobs.${c.id}.$t"] = u
        }
        return outputMap
    }

    private fun getNetWork(c: Container) = when (c) {
        is VMBuildContainer -> {
            if (c.dispatchType?.buildType() != BuildType.THIRD_PARTY_AGENT_ID &&
                c.dispatchType?.buildType() != BuildType.THIRD_PARTY_AGENT_ENV
            ) {
                "DEVNET"
            } else {
                "IDC"
            }
        }
        is NormalContainer -> {
            "IDC"
        }
        else -> {
            ""
        }
    }

    private fun getOs(c: Container) = when (c) {
        is VMBuildContainer -> {
            c.baseOS.name
        }
        is NormalContainer -> {
            VMBaseOS.LINUX.name
        }
        else -> {
            ""
        }
    }

    private fun getJobStatus(c: Container): String {
        return if (c is VMBuildContainer && c.status == BuildStatus.FAILED.name) {
            if (c.jobControlOption?.continueWhenFailed == true) {
                BuildStatus.SUCCEED.name
            } else {
                BuildStatus.FAILED.name
            }
        } else if (c is NormalContainer && c.status == BuildStatus.FAILED.name) {
            if (c.jobControlOption?.continueWhenFailed == true) {
                BuildStatus.SUCCEED.name
            } else {
                BuildStatus.FAILED.name
            }
        } else {
            c.status ?: ""
        }
    }

    private fun getStepStatus(e: Element): String {
        return if (e.status == BuildStatus.FAILED.name) {
            if (e.additionalOptions?.continueWhenFailed == true) {
                BuildStatus.SUCCEED.name
            } else {
                BuildStatus.FAILED.name
            }
        } else {
            e.status ?: ""
        }
    }
}
