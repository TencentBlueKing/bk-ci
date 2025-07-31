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

package com.tencent.devops.process.engine.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.WebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.webhook.util.WebhookUtils
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineResourceDao
import com.tencent.devops.process.engine.dao.PipelineWebhookDao
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum
import com.tencent.devops.process.pojo.webhook.PipelineWebhook
import com.tencent.devops.process.pojo.webhook.WebhookTriggerPipeline
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.Repository
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线webhook存储服务
 * @version 1.0
 */
@Suppress("ALL")
@Service
class PipelineWebhookService @Autowired constructor(
    private val scmProxyService: ScmProxyService,
    private val dslContext: DSLContext,
    private val pipelineWebhookDao: PipelineWebhookDao,
    private val pipelineResourceDao: PipelineResourceDao,
    private val objectMapper: ObjectMapper,
    private val client: Client,
    private val pipelinePermissionService: PipelinePermissionService,
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineWebhookService::class.java)
    }

    fun addWebhook(
        projectId: String,
        pipelineId: String,
        version: Int?,
        userId: String
    ) {
        val model = getModel(projectId, pipelineId, version)
        if (model == null) {
            logger.info("$pipelineId|$version|model is null")
            return
        }
        val triggerContainer = model.getTriggerContainer()
        val variables = PipelineVarUtil.fillVariableMap(
            triggerContainer.params.associate { param ->
                param.id to param.defaultValue.toString()
            }
        )
        val elements = triggerContainer.elements.filterIsInstance<WebHookTriggerElement>()
        val failedElementNames = mutableListOf<String>()
        elements.forEach { element ->
            try {
                saveWebhook(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    element = element,
                    variables = variables
                )
            } catch (ignore: Exception) {
                failedElementNames.add("- ${element.name}: ${ignore.message}")
                logger.warn("$projectId|$pipelineId|add webhook failed", ignore)
            }
        }
        sendNotify(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            pipelineName = model.name,
            failedElementNames = failedElementNames
        )
    }

    private fun saveWebhook(
        projectId: String,
        pipelineId: String,
        element: Element,
        variables: Map<String, String>
    ) {
        val (scmType, eventType, repositoryConfig) =
            RepositoryConfigUtils.buildWebhookConfig(element, variables)
        // 当事件触发代码库类型为self时,不需要注册webhook,因为保存时还不知道关联的代码库,只有发布时才知道
        if (repositoryConfig.repositoryType == RepositoryType.ID &&
            repositoryConfig.repositoryHashId.isNullOrBlank()
        ) {
            logger.warn("repositoryHashId is empty|$projectId|$pipelineId")
            return
        }
        logger.info("$pipelineId| Trying to add the $scmType web hook for repo($repositoryConfig)")
        val repository = registerWebhook(
            projectId = projectId,
            scmType = scmType,
            repositoryConfig = repositoryConfig,
            codeEventType = eventType,
            elementVersion = element.version
        ) ?: return
        val pipelineWebhook = PipelineWebhook(
            projectId = projectId,
            pipelineId = pipelineId,
            repositoryType = scmType,
            repoType = repositoryConfig.repositoryType,
            repoHashId = repositoryConfig.repositoryHashId,
            repoName = repositoryConfig.repositoryName,
            taskId = element.id,
            projectName = getExternalName(scmType = scmType, repository.projectName),
            repositoryHashId = repository.repoHashId,
            eventType = eventType?.name ?: "",
            externalId = repository.getExternalId(),
            externalName = getExternalName(scmType = scmType, repository.projectName)
        )
        pipelineWebhookDao.save(
            dslContext = dslContext,
            pipelineWebhook = pipelineWebhook
        )
    }

    fun registerWebhook(
        projectId: String,
        scmType: ScmType,
        repositoryConfig: RepositoryConfig,
        codeEventType: CodeEventType?,
        elementVersion: String
    ): Repository? {
        // 防止同一个仓库注册多个相同事件的webhook
        val redisLock = RedisLock(
            redisOperation = redisOperation,
            lockKey = "webhook:register:${repositoryConfig.getRepositoryId()}:$codeEventType",
            expiredTimeInSeconds = 30
        )
        try {
            redisLock.lock()
            return when (scmType) {
                ScmType.CODE_GIT ->
                    scmProxyService.addGitWebhook(
                        projectId = projectId,
                        repositoryConfig = repositoryConfig,
                        codeEventType = codeEventType
                    )

                ScmType.CODE_SVN ->
                    scmProxyService.addSvnWebhook(projectId = projectId, repositoryConfig = repositoryConfig)

                ScmType.CODE_GITLAB ->
                    scmProxyService.addGitlabWebhook(
                        projectId = projectId,
                        repositoryConfig = repositoryConfig,
                        codeEventType = codeEventType
                    )

                ScmType.GITHUB -> {
                    client.get(ServiceRepositoryResource::class).get(
                        projectId = projectId,
                        repositoryId = repositoryConfig.getURLEncodeRepositoryId(),
                        repositoryType = repositoryConfig.repositoryType
                    ).data!!
                }

                ScmType.CODE_TGIT -> {
                    scmProxyService.addTGitWebhook(
                        projectId = projectId,
                        repositoryConfig = repositoryConfig,
                        codeEventType = codeEventType
                    )
                }

                ScmType.CODE_P4 ->
                    if (WebhookUtils.isCustomP4TriggerVersion(elementVersion)) {
                        val repo = client.get(ServiceRepositoryResource::class).get(
                            projectId = projectId,
                            repositoryId = repositoryConfig.getURLEncodeRepositoryId(),
                            repositoryType = repositoryConfig.repositoryType
                        ).data!!
                        repo
                    } else {
                        scmProxyService.addP4Webhook(
                            projectId = projectId,
                            repositoryConfig = repositoryConfig,
                            codeEventType = codeEventType
                        )
                    }

                ScmType.SCM_GIT, ScmType.SCM_SVN -> {
                    scmProxyService.addScmWebhook(
                        projectId = projectId,
                        repositoryConfig = repositoryConfig,
                        codeEventType = codeEventType
                    )
                }

                else -> {
                    null
                }
            }
        } finally {
            redisLock.unlock()
        }
    }

    private fun sendNotify(
        userId: String,
        projectId: String,
        pipelineId: String,
        pipelineName: String,
        failedElementNames: List<String>
    ) {
        if (failedElementNames.isNotEmpty()) {
            client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(
                SendNotifyMessageTemplateRequest(
                    templateCode =
                    PipelineNotifyTemplateEnum.PIPELINE_WEBHOOK_REGISTER_FAILURE_NOTIFY_TEMPLATE.templateCode,
                    receivers = mutableSetOf(userId),
                    notifyType = mutableSetOf(NotifyType.RTX.name),
                    titleParams = mapOf("pipelineName" to pipelineName),
                    bodyParams = mapOf(
                        "pipelineName" to pipelineName,
                        "elementNames" to failedElementNames.joinToString(""),
                        "pipelineEditUrl" to pipelineEditUrl(projectId, pipelineId)
                    ),
                    cc = null,
                    bcc = null
                )
            )
        }
    }

    private fun pipelineEditUrl(projectId: String, pipelineId: String) =
        "${HomeHostUtil.innerServerHost()}/console/pipeline/$projectId/$pipelineId/edit"

    fun deleteWebhook(projectId: String, pipelineId: String, userId: String): Result<Boolean> {
        logger.info("delete $pipelineId webhook by $userId")
        pipelineWebhookDao.deleteByPipelineId(dslContext, projectId, pipelineId)
        return Result(true)
    }

    fun deleteWebhook(
        projectId: String,
        pipelineId: String,
        taskId: String,
        userId: String
    ): Result<Boolean> {
        logger.info("delete pipelineId:$pipelineId, taskId:$taskId webhook by $userId")
        pipelineWebhookDao.deleteByTaskId(dslContext, projectId, pipelineId, taskId)
        return Result(true)
    }

    fun getModel(projectId: String, pipelineId: String, version: Int? = null): Model? {
        val modelString =
            pipelineResourceDao.getVersionModelString(dslContext, projectId, pipelineId, version) ?: return null
        return try {
            objectMapper.readValue(modelString, Model::class.java)
        } catch (e: Exception) {
            logger.warn("get process($pipelineId) model fail", e)
            null
        }
    }

    fun getTriggerPipelines(
        name: String,
        repositoryType: ScmType,
        yamlPipelineIds: List<String>?,
        compatibilityRepoNames: Set<String>
    ): List<WebhookTriggerPipeline> {
        val pipelineSet = mutableSetOf<WebhookTriggerPipeline>()
        // 需要精确匹配的代码库类型
        val needExactMatch = repositoryType in setOf(
            ScmType.CODE_GIT,
            ScmType.CODE_TGIT,
            ScmType.CODE_GITLAB,
            ScmType.GITHUB
        ) && name != getProjectName(name)
        // 精准匹配结果
        val exactResults = if (needExactMatch) {
            pipelineWebhookDao.getByProjectNamesAndType(
                dslContext = dslContext,
                projectNames = setOf(name),
                repositoryType = repositoryType.name,
                yamlPipelineIds = yamlPipelineIds
            )?.toSet() ?: setOf()
        } else {
            setOf()
        }
        // 模糊匹配和兼容仓库名一起查
        val repoNames = compatibilityRepoNames.map { getProjectName(it) }.toMutableSet()
        repoNames.add(getProjectName(name))
        // 模糊匹配结果
        val fuzzyResults = pipelineWebhookDao.getByProjectNamesAndType(
            dslContext = dslContext,
            projectNames = repoNames,
            repositoryType = repositoryType.name,
            yamlPipelineIds = yamlPipelineIds
        )?.toSet() ?: setOf()
        // projectName字段补充完毕后，模糊匹配结果应为空
        if (needExactMatch && fuzzyResults.isNotEmpty()) {
            logger.info("$repositoryType|$name|projectName contains dirty data|${fuzzyResults.size}")
        }
        pipelineSet.addAll(exactResults)
        pipelineSet.addAll(fuzzyResults)
        return pipelineSet.toList()
    }

    fun listTriggerPipeline(
        projectId: String,
        repositoryHashId: String,
        eventType: String
    ): List<WebhookTriggerPipeline> {
        return pipelineWebhookDao.listTriggerPipeline(
            dslContext = dslContext,
            projectId = projectId,
            repositoryHashId = repositoryHashId,
            eventType = eventType
        ) ?: emptyList()
    }

    fun getProjectName(projectName: String): String {
        // 如果项目名是三层的，比如a/b/c，那对应的rep_name是b
        val repoSplit = projectName.split("/")
        if (repoSplit.size != 3) {
            return projectName
        }
        return repoSplit[1].trim()
    }

    /**
     * 获取代码库平台仓库名
     */
    fun getExternalName(scmType: ScmType, projectName: String): String {
        val repoSplit = projectName.split("/")
        // 如果代码库是svn类型，并且项目名是三层的，比如a/b/c，那对应的rep_name是b,工蜂svn webhook返回的rep_name结构
        if (scmType == ScmType.CODE_SVN && repoSplit.size == 3) {
            return repoSplit[1].trim()
        }
        return projectName
    }

    fun listWebhook(
        userId: String,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?
    ): List<PipelineWebhook> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 20
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        if (!pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.VIEW
            )
        ) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.USER_NEED_PROJECT_X_PERMISSION,
                params = arrayOf(userId, projectId)
            )
        }
        return pipelineWebhookDao.listWebhook(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            offset = limit.offset,
            limit = limit.limit
        ) ?: emptyList()
    }

    fun get(
        projectId: String,
        pipelineId: String,
        repositoryHashId: String,
        eventType: String
    ): PipelineWebhook? {
        return pipelineWebhookDao.get(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            repositoryHashId = repositoryHashId,
            eventType = eventType
        )
    }
}
