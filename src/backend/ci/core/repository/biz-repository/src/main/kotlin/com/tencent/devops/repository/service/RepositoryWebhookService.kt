package com.tencent.devops.repository.service

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.repository.dao.RepositoryWebhookRequestDao
import com.tencent.devops.repository.pojo.RepoCondition
import com.tencent.devops.repository.pojo.RepositoryWebhookRequest
import com.tencent.devops.repository.pojo.webhook.WebhookData
import com.tencent.devops.repository.pojo.webhook.WebhookEnrichRequest
import com.tencent.devops.repository.pojo.webhook.WebhookParseRequest
import com.tencent.devops.repository.service.code.CodeRepositoryManager
import com.tencent.devops.repository.service.hub.ScmWebhookApiService
import com.tencent.devops.scm.api.pojo.HookRequest
import com.tencent.devops.scm.api.pojo.webhook.Webhook
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@SuppressWarnings("ALL")
class RepositoryWebhookService @Autowired constructor(
    val dslContext: DSLContext,
    val repositoryWebhookRequestDao: RepositoryWebhookRequestDao,
    private val webhookApiService: ScmWebhookApiService,
    private val codeRepositoryManager: CodeRepositoryManager,
    private val repoPipelineService: RepoPipelineService
) {
    fun webhookParse(
        scmCode: String,
        request: WebhookParseRequest
    ): WebhookData? {
        val hookRequest = with(request) {
            HookRequest(headers, body, queryParams)
        }
        val webhook = webhookApiService.webhookParse(scmCode = scmCode, request = hookRequest) ?: run {
            logger.warn("Unsupported webhook request $scmCode [${JsonUtil.toJson(request, false)}]")
            return null
        }
        val serverRepo = webhook.repository()
        logger.info(
            "webhook parse result|scmCode:$scmCode|id:${serverRepo.id}|fullName:${serverRepo.fullName}"
        )
        val repoExternalId = serverRepo.id.toString()
        val condition = RepoCondition(projectName = serverRepo.fullName, gitProjectId = repoExternalId)
        val repositories = codeRepositoryManager.listByCondition(
            scmCode = scmCode,
            repoCondition = condition,
            offset = 0,
            limit = 500
        ) ?: emptyList()
        try {
            saveWebhookRequest(
                repositoryWebhookRequest = RepositoryWebhookRequest(
                    requestId = MDC.get(TraceTag.BIZID),
                    externalId = webhook.repository().id.toString(),
                    eventType = webhook.eventType,
                    triggerUser = webhook.userName,
                    eventMessage = "",
                    repositoryType = repositories.first().getScmType().name,
                    requestHeader = request.headers,
                    requestParam = request.queryParams,
                    requestBody = request.body,
                    createTime = LocalDateTime.now()
                )
            )
        } catch (ignored: Exception) {
            // 日志保存异常,不影响正常触发
            logger.warn("Failed to save webhook request", ignored)
        }

        return WebhookData(
            webhook = webhook,
            repositories = repositories
        )
    }

    fun webhookEnrich(
        projectId: String,
        repoHashId: String,
        request: WebhookEnrichRequest
    ): Webhook {
        with(request) {
            return try {
                webhookApiService.webhookEnrich(
                    projectId = projectId,
                    repoHashId = repoHashId,
                    webhook = webhook
                )
            } catch (ignored: Exception) {
                logger.warn(
                    "Failed to enrich webhook|$projectId|$repoHashId|${webhook.repository().fullName}", ignored
                )
                webhook
            }
        }
    }

    fun saveWebhookRequest(repositoryWebhookRequest: RepositoryWebhookRequest) {
        with(repositoryWebhookRequest) {
            if (externalId.isBlank()) {
                throw ParamBlankException("Invalid eventSource")
            }
            if (repositoryType.isBlank()) {
                throw ParamBlankException("Invalid triggerType")
            }
            if (eventType.isBlank()) {
                throw ParamBlankException("Invalid eventType")
            }
            repositoryWebhookRequestDao.saveWebhookRequest(
                dslContext = dslContext,
                webhookRequest = repositoryWebhookRequest
            )
        }
    }

    fun getWebhookRequest(requestId: String): RepositoryWebhookRequest? {
        return repositoryWebhookRequestDao.get(
            dslContext = dslContext,
            requestId = requestId
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RepositoryWebhookService::class.java)
    }
}
