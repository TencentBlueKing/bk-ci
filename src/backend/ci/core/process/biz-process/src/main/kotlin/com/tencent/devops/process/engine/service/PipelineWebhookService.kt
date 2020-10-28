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

package com.tencent.devops.process.engine.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineResDao
import com.tencent.devops.process.engine.dao.PipelineWebhookDao
import com.tencent.devops.process.engine.pojo.PipelineWebhook
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.process.util.WebhookRedisUtils
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.Repository
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

/**
 * 流水线webhook存储服务
 * @version 1.0
 */
@Service
class PipelineWebhookService @Autowired constructor(
    private val webhookRedisUtils: WebhookRedisUtils,
    private val scmProxyService: ScmProxyService,
    private val dslContext: DSLContext,
    private val pipelineWebhookDao: PipelineWebhookDao,
    private val pipelineResDao: PipelineResDao,
    private val objectMapper: ObjectMapper,
    private val client: Client
) {

    private val logger = LoggerFactory.getLogger(javaClass)!!

    fun saveWebhook(
        pipelineWebhook: PipelineWebhook,
        codeEventType: CodeEventType? = null,
        variables: Map<String, String>? = null,
        createPipelineFlag: Boolean? = false
    ): Result<Boolean> {
        logger.info("save Webhook[$pipelineWebhook]")
        val repositoryConfig = getRepositoryConfig(pipelineWebhook, variables)

        var continueFlag = true
        if (createPipelineFlag != null && createPipelineFlag) {
            // 新增流水线时，模版里配置的代码库是变量或者当前项目下不存在，不需创建webhook
            try {
                scmProxyService.getRepo(pipelineWebhook.projectId, repositoryConfig)
            } catch (e: Exception) {
                logger.info("skip save Webhook[$pipelineWebhook]: ${e.message}")
                continueFlag = false
            }
        }

        if (continueFlag) {
            val projectName = when (pipelineWebhook.repositoryType) {
                ScmType.CODE_GIT ->
                    scmProxyService.addGitWebhook(pipelineWebhook.projectId, repositoryConfig, codeEventType)
                ScmType.CODE_SVN ->
                    scmProxyService.addSvnWebhook(pipelineWebhook.projectId, repositoryConfig)
                ScmType.CODE_GITLAB ->
                    scmProxyService.addGitlabWebhook(pipelineWebhook.projectId, repositoryConfig)
                ScmType.GITHUB -> {
                    val repo = client.get(ServiceRepositoryResource::class).get(
                        pipelineWebhook.projectId,
                        repositoryConfig.getURLEncodeRepositoryId(),
                        repositoryConfig.repositoryType
                    ).data!!
                    repo.projectName
                }
                ScmType.CODE_TGIT -> {
                    scmProxyService.addTGitWebhook(pipelineWebhook.projectId, repositoryConfig, codeEventType)
                }
                else -> {
                    null
                }
            }
            logger.info("add $projectName webhook to [$pipelineWebhook]")
            if (!projectName.isNullOrBlank()) {
                pipelineWebhook.projectName = projectName
                saveOrUpdateWebhook(pipelineWebhook)
                webhookRedisUtils.addWebhook2Redis(
                    pipelineWebhook.pipelineId,
                    projectName!!,
                    pipelineWebhook.repositoryType,
                    ::getExistWebhookPipelineByType
                )
            }
        }
        return Result(true)
    }

    fun saveWebhook(
        pipelineWebhook: PipelineWebhook,
        repo: Repository,
        codeEventType: CodeEventType? = null,
        hookUrl: String? = null,
        token: String? = null
    ) {
        logger.info("save generic Webhook[$pipelineWebhook]")
        scmProxyService.addGenericWebhook(
            projectId = pipelineWebhook.projectId,
            repo = repo,
            scmType = pipelineWebhook.repositoryType,
            codeEventType = codeEventType,
            hookUrl = hookUrl,
            token = token
        )
        saveOrUpdateWebhook(pipelineWebhook)
    }

    fun saveOrUpdateWebhook(
        pipelineWebhook: PipelineWebhook
    ) {
        val record = pipelineWebhookDao.getByPipelineAndTaskId(
            dslContext = dslContext,
            pipelineId = pipelineWebhook.pipelineId,
            taskId = pipelineWebhook.taskId!!
        )
        if (record == null) {
            pipelineWebhookDao.save(
                dslContext = dslContext,
                pipelineWebhook = pipelineWebhook
            )
        } else {
            pipelineWebhookDao.update(
                dslContext = dslContext,
                pipelineWebhook = pipelineWebhook
            )
        }
    }

    fun deleteWebhook(pipelineId: String, userId: String): Result<Boolean> {
        logger.info("delete $pipelineId webhook by $userId")
        pipelineWebhookDao.delete(dslContext, pipelineId)
        return Result(true)
    }

    fun deleteWebhook(
        pipelineId: String,
        taskId: String,
        userId: String
    ): Result<Boolean> {
        logger.info("delete pipelineId:$pipelineId, taskId:$taskId webhook by $userId")
        pipelineWebhookDao.delete(dslContext, pipelineId, taskId)
        return Result(true)
    }

    fun listRepositoryTypeWebhooks(
        repositoryType: ScmType,
        start: Int,
        limit: Int
    ): Result<Collection<PipelineWebhook>> {
        if (start < 0) {
            return Result(emptyList())
        }
        val list = pipelineWebhookDao.getPipelineWebHooksByRepositoryType(dslContext, repositoryType.name, start, limit)
        val pipelineWebhookList = mutableListOf<PipelineWebhook>()
        list.forEach {
            pipelineWebhookList.add(pipelineWebhookDao.convert(it))
        }
        return Result(pipelineWebhookList)
    }

    private fun getExistWebhookPipelineByType(
        type: ScmType
    ): HashMap<String, Set<String>> {
        val map = HashMap<String, Set<String>>()

        val pipelineVariables = HashMap<String, Map<String, String>>()
        var start = 0
        loop@ while (true) { // 新款
            val typeWebhooksResp = listRepositoryTypeWebhooks(type, start, 100)
            if (typeWebhooksResp.isNotOk() || typeWebhooksResp.data == null || typeWebhooksResp.data!!.isEmpty()) {
                break@loop
            }
            typeWebhooksResp.data!!.forEach {
                try {
                    var variables = pipelineVariables[it.pipelineId]
                    if (variables == null) {
                        variables = getStartupParam(it.pipelineId)
                        pipelineVariables[it.pipelineId] = variables
                    }
                    val repositoryConfig = getRepositoryConfig(it, variables)
                    val repo = client.get(ServiceRepositoryResource::class)
                        .get(it.projectId, repositoryConfig.getURLEncodeRepositoryId(), repositoryConfig.repositoryType)
                        .data
                    if (repo == null) {
                        logger.warn("repo[${it.repoHashId}] does not exist")
                        return@forEach
                    }
                    val projectName = getProjectName(repo.projectName)
                    val pipelineId = it.pipelineId
                    map.compute(projectName) { _, u -> u?.plus(pipelineId) ?: setOf(pipelineId) }
                } catch (t: Throwable) {
                    logger.warn("Fail to get repository - ($it), ignore", t)
                }
            }
            start += 100
        }

        return map
    }

    private fun getStartupParam(pipelineId: String): Map<String, String> {
        val result = HashMap<String, String>()
        val model = getModel(pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                defaultMessage = "流水线不存在"
            )

        val triggerContainer = model.stages[0].containers[0] as TriggerContainer

        triggerContainer.params.forEach {
            result[it.id] = it.defaultValue.toString()
        }
        logger.info("[$pipelineId] Get the pipeline startup param - ($result)")
        return result
    }

    fun getModel(pipelineId: String, version: Int? = null): Model? {
        val modelString = pipelineResDao.getVersionModelString(dslContext, pipelineId, version) ?: return null
        return try {
            objectMapper.readValue(modelString, Model::class.java)
        } catch (e: Exception) {
            logger.error("get process($pipelineId) model fail", e)
            null
        }
    }

    fun getWebhookPipelines(name: String, type: String): Set<String> {
        return webhookRedisUtils.getWebhookPipelines(name, type, ::getExistWebhookPipelineByType)
    }

    fun getRelativePath(url: String): String {
        val urlArray = url.split("//")
        if (urlArray.size < 2) {
            return ""
        }

        val path = urlArray[1]
        val repoSplit = path.split("/")
        if (repoSplit.size < 4) {
            return ""
        }
        val domain = repoSplit[0]
        val first = repoSplit[1]
        val second = repoSplit[2]

        return path.removePrefix("$domain/$first/$second").removePrefix("/")
    }

    fun getProjectName(projectName: String): String {
        // 如果项目名是三层的，比如a/b/c，那对应的rep_name是b
        val repoSplit = projectName.split("/")
        if (repoSplit.size != 3) {
            return projectName
        }
        return repoSplit[1].trim()
    }

    private fun getRepositoryConfig(
        pipelineWebhook: PipelineWebhook,
        variable: Map<String, String>? = null
    ): RepositoryConfig {
        return getRepositoryConfig(
            repoHashId = pipelineWebhook.repoHashId,
            repoName = pipelineWebhook.repoName,
            repoType = pipelineWebhook.repoType,
            variable = variable
        )
    }

    private fun getRepositoryConfig(
        repoHashId: String?,
        repoName: String?,
        repoType: RepositoryType?,
        variable: Map<String, String>? = null
    ): RepositoryConfig {
        return when (repoType) {
            RepositoryType.ID -> RepositoryConfig(repoHashId, null, RepositoryType.ID)
            RepositoryType.NAME -> {
                val repositoryName = if (variable == null || variable.isEmpty()) {
                    repoName!!
                } else {
                    EnvUtils.parseEnv(repoName!!, variable)
                }
                RepositoryConfig(null, repositoryName, RepositoryType.NAME)
            }
            else -> {
                if (!repoHashId.isNullOrBlank()) {
                    RepositoryConfig(repoHashId, null, RepositoryType.ID)
                } else if (!repoName.isNullOrBlank()) {
                    val repositoryName = if (variable == null || variable.isEmpty()) {
                        repoName!!
                    } else {
                        EnvUtils.parseEnv(repoName!!, variable)
                    }
                    RepositoryConfig(null, repositoryName, RepositoryType.NAME)
                } else {
                    // 两者不能同时为空
                    throw ErrorCodeException(
                        defaultMessage = "Webhook 的ID和名称同时为空",
                        errorCode = ProcessMessageCode.ERROR_PARAM_WEBHOOK_ID_NAME_ALL_NULL
                    )
                }
            }
        }
    }

    /**
     * 批量更新TASK_ID和PROJECT_NAME
     */
    fun updateProjectNameAndTaskId() {
        var start = 0
        loop@ while (true) {
            val pipelineIds = pipelineWebhookDao.groupByPipelineId(
                dslContext = dslContext,
                offset = start,
                limit = 100
            )
            if (pipelineIds == null || pipelineIds.isEmpty()) {
                break@loop
            }

            pipelineIds.forEach { (projectId, pipelineId) ->
                val model = getModel(pipelineId = pipelineId) ?: return@forEach
                val triggerContainer = model.stages[0].containers[0] as TriggerContainer
                val variable = triggerContainer.params.associate { it.id to it.defaultValue.toString() }
                val pipelineWebhooks = mutableListOf<PipelineWebhook>()
                triggerContainer.elements.forEach element@{ element ->
                    val (repositoryConfig, repositoryType, originRepoName) = getRepositoryConfig(element, variable)
                        ?: return@element
                    try {
                        val repo = client.get(ServiceRepositoryResource::class)
                            .get(
                                projectId,
                                repositoryConfig.getURLEncodeRepositoryId(),
                                repositoryConfig.repositoryType
                            ).data
                        if (repo == null) {
                            logger.warn("pipelineId:[$pipelineId],repo[$repositoryConfig] does not exist")
                            return@element
                        }
                        pipelineWebhooks.add(
                            PipelineWebhook(
                                projectId = projectId,
                                pipelineId = pipelineId,
                                repositoryType = repositoryType,
                                repoType = repositoryConfig.repositoryType,
                                repoHashId = repositoryConfig.repositoryHashId,
                                repoName = originRepoName,
                                projectName = getProjectName(repo.projectName),
                                taskId = element.id
                            )
                        )
                    } catch (t: Throwable) {
                        logger.warn("pipelineId:[$pipelineId], Fail to get repository - repositoryConfig:($repositoryConfig), repositoryType:($repositoryType), ignore")
                    }
                }
                pipelineWebhookDao.updateProjectNameAndTaskId(
                    dslContext = dslContext,
                    pipelinewebhooks = pipelineWebhooks
                )
            }
            start += 100
        }
    }

    private fun getRepositoryConfig(
        element: Element,
        variable: Map<String, String>
    ): Triple<RepositoryConfig, ScmType, String?>? {
        return when (element) {
            is CodeGitWebHookTriggerElement ->
                Triple(
                    getRepositoryConfig(
                        repoHashId = element.repositoryHashId,
                        repoName = element.repositoryName,
                        repoType = element.repositoryType,
                        variable = variable
                    ),
                    ScmType.CODE_GIT,
                    element.repositoryName
                )
            is CodeGithubWebHookTriggerElement ->
                Triple(
                    getRepositoryConfig(
                        repoHashId = element.repositoryHashId,
                        repoName = element.repositoryName,
                        repoType = element.repositoryType,
                        variable = variable
                    ),
                    ScmType.GITHUB,
                    element.repositoryName
                )
            is CodeGitlabWebHookTriggerElement ->
                Triple(
                    getRepositoryConfig(
                        repoHashId = element.repositoryHashId,
                        repoName = element.repositoryName,
                        repoType = element.repositoryType,
                        variable = variable
                    ),
                    ScmType.CODE_GITLAB,
                    element.repositoryName
                )
            is CodeSVNWebHookTriggerElement ->
                Triple(
                    getRepositoryConfig(
                        repoHashId = element.repositoryHashId,
                        repoName = element.repositoryName,
                        repoType = element.repositoryType,
                        variable = variable
                    ),
                    ScmType.CODE_SVN,
                    element.repositoryName
                )

            else -> null
        }
    }
}
