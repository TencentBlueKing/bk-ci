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
 *
 */

package com.tencent.devops.process.engine.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitlabElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeSvnElement
import com.tencent.devops.common.pipeline.pojo.element.agent.GithubElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeP4WebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.process.engine.dao.PipelineModelTaskDao
import com.tencent.devops.process.engine.dao.PipelineResDao
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.RepoPipelineRefInfo
import com.tencent.devops.repository.pojo.RepoPipelineRefRequest
import com.tencent.devops.repository.pojo.enums.RepoAtomCategoryEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 流水线代码库使用服务
 * @version 1.0
 */
@Suppress("ALL")
@Service
class RepoPipelineRefService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineResDao: PipelineResDao,
    private val objectMapper: ObjectMapper,
    private val client: Client,
    private val modelTaskDao: PipelineModelTaskDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RepoPipelineRefService::class.java)
        private val repoAtomCodes = listOf(
            "codeGitWebHookTrigger", "codeSVNWebHookTrigger", "codeGitlabWebHookTrigger",
            "codeGithubWebHookTrigger", "codeTGitWebHookTrigger", "codeP4WebHookTrigger",
            "gitCodeRepo", "PullFromGithub", "Gitlab", "atomtgit", "checkout", "svnCodeRepo", "PerforceSync"
        )
        // 代码库拉取插件
        private val repoCheckoutAtomCodes =
            setOf("gitCodeRepo", "PullFromGithub", "Gitlab", "atomtgit", "checkout", "svnCodeRepo")
    }

    fun updateRepoPipelineRef(
        userId: String,
        action: String,
        projectId: String,
        pipelineId: String
    ) {
        logger.info("updateRepositoryPipelineRef, [$userId|$action|$projectId|$pipelineId]")
        var model: Model? = null
        if (action != "delete_pipeline") {
            val modelString = pipelineResDao.getLatestVersionModelString(dslContext, projectId, pipelineId)
            if (modelString.isNullOrBlank()) {
                logger.warn("model not found: [$userId|$action|$projectId|$pipelineId]")
                return
            }
            try {
                model = objectMapper.readValue(modelString, Model::class.java)
            } catch (ignored: Exception) {
                logger.error("parse process($pipelineId) model fail", ignored)
                return
            }
        }
        try {
            analysisPipelineRefAndSave(userId, action, projectId, pipelineId, model)
        } catch (e: Exception) {
            logger.error("analysisPipelineRefAndSave failed", e)
        }
    }

    fun updatePipelineRef(userId: String, projectId: String, pipelineId: String) {
        logger.info("update pipeline repository ref|$userId|$projectId|$pipelineId")
        updateRepoPipelineRef(userId, "op", projectId, pipelineId)
    }

    fun updateAllPipelineRef() {
        val startTime = System.currentTimeMillis()
        logger.info("op update all pipeline repository ref")
        val threadPoolExecutor = ThreadPoolExecutor(
            1,
            1,
            0,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(1),
            Executors.defaultThreadFactory(),
            ThreadPoolExecutor.AbortPolicy()
        )
        threadPoolExecutor.submit {
            logger.info("Start to update pipeline repository ref")
            var offset = 0
            val limit = 1000
            try {
                do {
                    val records = modelTaskDao.batchGetPipelineIdByAtomCode(
                        dslContext = dslContext,
                        atomCodeList = repoAtomCodes,
                        limit = limit,
                        offset = offset
                    )
                    val count = records?.size ?: 0
                    records?.map {
                        try {
                            updatePipelineRef(
                                userId = "op",
                                projectId = it.value1(),
                                pipelineId = it.value2()
                            )
                        } catch (ignored: Exception) {
                            logger.warn(
                                "Failed to update pipeline repository ref|${it.value1()}|${it.value2()}",
                                ignored
                            )
                        }
                    }
                    offset += limit
                } while (count == 1000)
            } catch (ignored: Exception) {
                logger.warn("Failed to update pipeline repository ref", ignored)
            } finally {
                logger.info("Finish to update pipeline repository ref|${System.currentTimeMillis() - startTime}")
                threadPoolExecutor.shutdown()
            }
        }
    }

    private fun analysisPipelineRefAndSave(
        userId: String,
        action: String,
        projectId: String,
        pipelineId: String,
        model: Model?
    ) {
        val repoPipelineRefInfos = mutableListOf<RepoPipelineRefInfo>()
        val variables = mutableMapOf<String, String>()
        model?.stages?.forEachIndexed { index, stage ->
            if (index == 0) {
                val container = stage.containers[0] as TriggerContainer
                // 解析变量
                container.params.forEach { param ->
                    variables[param.id] = param.defaultValue.toString()
                }
                analysisTriggerContainer(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    pipelineName = model.name,
                    container = container,
                    variables = variables,
                    repoPipelineRefInfos = repoPipelineRefInfos
                )
            } else {
                analysisOtherContainer(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    pipelineName = model.name,
                    stage = stage,
                    variables = variables,
                    repoPipelineRefInfos = repoPipelineRefInfos
                )
            }
        }
        client.get(ServiceRepositoryResource::class).updatePipelineRef(
            userId = userId,
            projectId = projectId,
            request = RepoPipelineRefRequest(
                action = action,
                pipelineId = pipelineId,
                pipelineRefInfos = repoPipelineRefInfos
            )
        )
    }

    private fun analysisTriggerContainer(
        projectId: String,
        pipelineId: String,
        pipelineName: String,
        container: TriggerContainer,
        variables: Map<String, String>,
        repoPipelineRefInfos: MutableList<RepoPipelineRefInfo>
    ) {
        container.elements.forEach e@{ element ->
            val repositoryConfig = when (element) {
                is CodeGitWebHookTriggerElement -> RepositoryConfig(
                    repositoryHashId = element.repositoryHashId,
                    repositoryName = EnvUtils.parseEnv(element.repositoryName, variables),
                    repositoryType = element.repositoryType ?: RepositoryType.ID
                )

                is CodeSVNWebHookTriggerElement -> RepositoryConfig(
                    repositoryHashId = element.repositoryHashId,
                    repositoryName = EnvUtils.parseEnv(element.repositoryName, variables),
                    repositoryType = element.repositoryType ?: RepositoryType.ID
                )

                is CodeGitlabWebHookTriggerElement -> RepositoryConfig(
                    repositoryHashId = element.repositoryHashId,
                    repositoryName = EnvUtils.parseEnv(element.repositoryName, variables),
                    repositoryType = element.repositoryType ?: RepositoryType.ID
                )

                is CodeGithubWebHookTriggerElement -> RepositoryConfig(
                    repositoryHashId = element.repositoryHashId,
                    repositoryName = EnvUtils.parseEnv(element.repositoryName, variables),
                    repositoryType = element.repositoryType ?: RepositoryType.ID
                )

                is CodeTGitWebHookTriggerElement -> RepositoryConfig(
                    repositoryHashId = element.data.input.repositoryHashId,
                    repositoryName = EnvUtils.parseEnv(element.data.input.repositoryName, variables),
                    repositoryType = element.data.input.repositoryType ?: RepositoryType.ID
                )

                is CodeP4WebHookTriggerElement -> RepositoryConfig(
                    repositoryHashId = element.data.input.repositoryHashId,
                    repositoryName = EnvUtils.parseEnv(element.data.input.repositoryName, variables),
                    repositoryType = element.data.input.repositoryType ?: RepositoryType.ID
                )

                else -> return@e
            }

            repoPipelineRefInfos.add(
                RepoPipelineRefInfo(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    pipelineName = pipelineName,
                    repositoryConfig = repositoryConfig,
                    taskId = element.id!!,
                    taskName = element.name,
                    taskParams = element.genTaskParams(),
                    atomCode = element.getAtomCode(),
                    atomVersion = element.version,
                    atomCategory = RepoAtomCategoryEnum.TRIGGER.name
                )
            )
        }
    }

    private fun analysisOtherContainer(
        projectId: String,
        pipelineId: String,
        pipelineName: String,
        stage: Stage,
        variables: Map<String, String>,
        repoPipelineRefInfos: MutableList<RepoPipelineRefInfo>
    ) {
        stage.containers.forEach c@{ container ->
            if (container is TriggerContainer) {
                return@c
            }
            container.elements.forEach e@{ element ->
                val repositoryConfig = when {
                    element is CodeGitElement -> RepositoryConfig(
                        repositoryHashId = element.repositoryHashId,
                        repositoryName = EnvUtils.parseEnv(element.repositoryName, variables),
                        repositoryType = element.repositoryType ?: RepositoryType.ID
                    )

                    element is CodeSvnElement -> RepositoryConfig(
                        repositoryHashId = element.repositoryHashId,
                        repositoryName = EnvUtils.parseEnv(element.repositoryName, variables),
                        repositoryType = element.repositoryType ?: RepositoryType.ID
                    )

                    element is CodeGitlabElement -> RepositoryConfig(
                        repositoryHashId = element.repositoryHashId,
                        repositoryName = EnvUtils.parseEnv(element.repositoryName, variables),
                        repositoryType = element.repositoryType ?: RepositoryType.ID
                    )

                    element is GithubElement -> RepositoryConfig(
                        repositoryHashId = element.repositoryHashId,
                        repositoryName = EnvUtils.parseEnv(element.repositoryName, variables),
                        repositoryType = element.repositoryType ?: RepositoryType.ID
                    )

                    element is MarketBuildAtomElement && element.getAtomCode() in repoCheckoutAtomCodes -> {
                        val input = element.data["input"]
                        if (input !is Map<*, *>) return@e
                        getMarketBuildRepoConfig(input = input, variables = variables) ?: return@e
                    }

                    else -> return@e
                }
                repoPipelineRefInfos.add(
                    RepoPipelineRefInfo(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        pipelineName = pipelineName,
                        repositoryConfig = repositoryConfig,
                        taskId = element.id!!,
                        taskName = element.name,
                        taskParams = element.genTaskParams(),
                        atomCode = element.getAtomCode(),
                        atomVersion = element.version,
                        atomCategory = RepoAtomCategoryEnum.TRIGGER.name
                    )
                )
            }
        }
    }

    private fun getMarketBuildRepoConfig(input: Map<*, *>, variables: Map<String, String>): RepositoryConfig? {
        val repositoryType = RepositoryType.parseType(input["repositoryType"] as String?)
        val repositoryId = when (repositoryType) {
            RepositoryType.ID -> EnvUtils.parseEnv(input["repositoryHashId"] as String?, variables)
            RepositoryType.NAME -> EnvUtils.parseEnv(input["repositoryName"] as String?, variables)
            else -> return null
        }
        return RepositoryConfigUtils.buildConfig(repositoryId, repositoryType)
    }
}
