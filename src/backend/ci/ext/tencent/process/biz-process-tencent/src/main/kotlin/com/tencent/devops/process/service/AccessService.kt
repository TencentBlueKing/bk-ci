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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.element.SingleArchiveElement
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.CodePullStrategy
import com.tencent.devops.common.pipeline.enums.GitPullModeType
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.pojo.coverity.ProjectLanguage
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitElement
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.atom.LinuxPaasCodeCCScriptElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.git.GitPullMode
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.process.engine.service.PipelineService
import com.tencent.devops.process.pojo.AccessRepository
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.ticket.api.ServiceCredentialResource
import com.tencent.devops.ticket.pojo.CredentialCreate
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AccessService @Autowired constructor(
    private val client: Client,
    private val pipelineService: PipelineService
) {

    @Value("\${git.cmake}")
    private val CMAKE_REPO_URL = ""

    @Value("\${git.token}")
    private val GIT_TOKEN = ""
    @Value("\${git.ssh.key}")
    private val GIT_SSH_KEY = ""

    fun create(userId: String, accessRepository: AccessRepository): String {
        val timestamp = LocalDateTime.now().timestamp()
        val projectId = accessRepository.projectId
        val url = CMAKE_REPO_URL
        val hashCode = HashUtil.encodeLongId(timestamp)

        val credentialId = "git_credential_$hashCode"
        val credentialRemark = "新手接入凭据"
        createCredential(userId, projectId, credentialId, credentialRemark)

        val projectName = "bkdevops/cmake"
        val repositoryAliasName = "Code Demo（新手接入专用）_$hashCode"
        val repositoryHashId = createRepository(userId, projectId, projectName, credentialId, url, repositoryAliasName)

        val pipelineName = "新手接入_$hashCode"
        return createPipeline(pipelineName, userId, projectId, repositoryHashId)
    }

    private fun createCredential(userId: String, projectId: String, credentialId: String, credentialRemark: String) {
        try {
            client.get(ServiceCredentialResource::class).check(projectId, credentialId)
        } catch (exception: RemoteServiceException) {
            if (exception.httpStatus != 404) {
                throw exception
            }
            val credentialType = CredentialType.TOKEN_SSH_PRIVATEKEY
            val credentialCreate = CredentialCreate(credentialId, credentialType, credentialRemark, GIT_TOKEN, GIT_SSH_KEY, null, null)
            client.get(ServiceCredentialResource::class).create(userId, projectId, credentialCreate)
        }
    }

    private fun createRepository(userId: String, projectId: String, projectName: String, credentialId: String, url: String, aliasName: String): String {
        val repository = CodeGitRepository(aliasName, url, credentialId, projectName, userId, RepoAuthType.SSH, projectId, null)
        return client.get(ServiceRepositoryResource::class).create(userId, projectId, repository).data!!.hashId
    }

    private fun createPipeline(pipelineName: String, userId: String, projectId: String, repositoryHashId: String): String {
        return createCmakePipeline(pipelineName, userId, projectId, repositoryHashId)
    }

    private fun createCmakePipeline(pipelineName: String, userId: String, projectId: String, repositoryHashId: String): String {
        val model = generateModel(
                pipelineName,
                "C++（CMake）构建",
                repositoryHashId,
                "",
                "cmake .\r\nmake",
                "make clean\r\ncmake .\r\nmake",
                "cmake")
        return pipelineService.createPipeline(userId, projectId, model, ChannelCode.BS)
    }

    private fun generateModel(name: String, desc: String, repositoryHashId: String, repositoryPath: String, codeCCScript: String, script: String, archivePath: String): Model {
        var containerSeqId = 0
        // stage-1
        val stageFirstElement = ManualTriggerElement(
                "手动触发",
                "T-1-1-1",
                null)
        val stageFirstElements = listOf<Element>(stageFirstElement)
        val stageFirstContainer = TriggerContainer(containerSeqId.toString(),
                "构建触发",
                stageFirstElements,
                null,
                null,
                null,
                null,
                emptyList(), null, null)
        containerSeqId++
        val stageFirstContainers = listOf<Container>(stageFirstContainer)
        val stageFirst = Stage(stageFirstContainers, "stage-1")

        // stage-2
        val stageSecondPullCodeElement = CodeGitElement(
                "拉取Git仓库代码",
                "T-2-1-1",
                null,
                repositoryHashId,
                "",
                "",
                CodePullStrategy.FRESH_CHECKOUT,
                repositoryPath, true, GitPullMode(GitPullModeType.BRANCH, "master"))
        val stageSecondCodeCCElement = LinuxPaasCodeCCScriptElement(
                "CodeCC代码检查任务",
                "T-2-1-2",
                null,
                BuildScriptType.SHELL,
                codeCCScript,
                null,
                null,
                null,
                false,
                "1",
                repositoryPath,
                listOf(ProjectLanguage.C_CPP)
        )
        stageSecondCodeCCElement.tools = listOf("COVERITY")
        val stageSecondLinuxScriptElement = LinuxScriptElement(
                "执行Linux脚本",
                "T-2-1-3",
                null,
                BuildScriptType.SHELL,
                script,
                false,
                false,
                null)
        val stageSecondSingleArchiveElement = SingleArchiveElement(
                "归档构件",
                "T-2-1-4",
                null,
                archivePath,
                "./",
                false)
        val stageSecondElements = listOf(stageSecondPullCodeElement, stageSecondCodeCCElement, stageSecondLinuxScriptElement, stageSecondSingleArchiveElement)

        // buildEnv
        val buildEnv = mutableMapOf<String, String>()
        buildEnv["cmake"] = "3.9.6"

        val stageSecondContainer = VMBuildContainer(
            id = containerSeqId.toString(),
            name = "构建环境",
            elements = stageSecondElements,
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            baseOS = VMBaseOS.LINUX,
            vmNames = emptySet(),
            maxQueueMinutes = 60,
            maxRunningMinutes = 480,
            buildEnv = buildEnv,
            customBuildEnv = null,
            thirdPartyAgentId = null,
            thirdPartyAgentEnvId = null,
            thirdPartyWorkspace = null,
            dockerBuildVersion = null,
            tstackAgentId = null,
            dispatchType = DockerDispatchType("tlinux2.2")
        )
        containerSeqId++
        val stageSecondContainers = listOf<Container>(stageSecondContainer)
        val stageSecond = Stage(stageSecondContainers, "stage-2")

        val stages = mutableListOf(stageFirst, stageSecond)
        return Model(name, desc, stages)
    }
}
