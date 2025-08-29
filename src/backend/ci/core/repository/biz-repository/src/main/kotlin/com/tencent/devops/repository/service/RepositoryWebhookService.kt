package com.tencent.devops.repository.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.repository.constant.RepositoryMessageCode.ERROR_WEBHOOK_SERVER_REPO_FULL_NAME_IS_EMPTY
import com.tencent.devops.repository.dao.RepositoryWebhookRequestDao
import com.tencent.devops.repository.pojo.RepoCondition
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryWebhookRequest
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.repository.pojo.webhook.WebhookData
import com.tencent.devops.repository.pojo.webhook.WebhookParseRequest
import com.tencent.devops.repository.service.code.CodeRepositoryManager
import com.tencent.devops.repository.service.hub.ScmWebhookApiService
import com.tencent.devops.scm.api.pojo.HookRequest
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

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
        if (serverRepo.fullName.isNullOrBlank()) {
            throw ErrorCodeException(
                errorCode = ERROR_WEBHOOK_SERVER_REPO_FULL_NAME_IS_EMPTY
            )
        }
        val repoExternalId = serverRepo.id?.toString()
        val condition = RepoCondition(projectName = serverRepo.fullName, gitProjectId = repoExternalId)
        val repositories =
            codeRepositoryManager.listByCondition(
                scmCode = scmCode,
                repoCondition = condition,
                offset = 0,
                limit = 500
            ) ?: emptyList()

        // 循环查找有权限的代码库,调用接口扩展webhook数据
        var enWebhook = webhook
        // 去重，相同的auth判断一次即可
        val repoList = sortedRepository(repositories).map { AuthRepository(it) }.distinctBy { it.auth }
        // 是否全部过期
        var allExpired = true
        for (repository in repoList) {
            try {
                enWebhook = webhookApiService.webhookEnrich(
                    webhook = webhook,
                    authRepo = repository
                )
                allExpired = false
                break
            } catch (ignored: RemoteServiceException) {
                when (ignored.errorCode) {
                    401 -> {
                        logger.warn(
                            "repository auth has expired|${repository.auth}", ignored
                        )
                    }

                    else -> {
                        logger.warn(
                            "fail to enrich webhook|${repository.auth}", ignored
                        )
                    }
                }
            } catch (ignored: Exception) {
                logger.warn(
                    "fail to enrich webhook|${repository.auth}", ignored
                )
            }
        }
        // 所有代码库都尝试失败,则返回原始数据
        if (allExpired) {
            logger.info(
                "all repository auth attempts failed, return original webhook data|scmCode:$scmCode|id:${serverRepo.id}|" +
                        "fullName:${serverRepo.fullName}"
            )
        }
        return WebhookData(
            webhook = enWebhook,
            repositories = repositories
        )
    }

    /**
     * 按照流水线触发器引用数量和代码库是否开启PAC来排序
     *
     * 一个代码仓库可能绑定蓝盾多个代码库,有些代码库的授权身份已经过期,会导致调用scm api接口报错，为了减少尝试次数
     * - 如果流水线触发器使用的越多,说明仓库约活跃,授权身份一般不会过期;
     * - 如果开启PAC的仓库授权身份过期,那么PAC的流水线都会报错,所以授权身份也不能过期
     */
    private fun sortedRepository(repositories: List<Repository>): List<Repository> {
        val pipelineRefCountMap = mutableMapOf<String, Int>()
        // 数据库索引是项目ID+代码库ID,所以按照项目ID分组后再查询
        repositories.groupBy { it.projectId }.forEach { (projectId, repos) ->
            repoPipelineService.countPipelineRefs(
                projectId!!,
                repos.map { HashUtil.decodeOtherIdToLong(it.repoHashId!!) }
            ).forEach { (repositoryId, cnt) ->
                pipelineRefCountMap[HashUtil.encodeOtherLongId(repositoryId)] = cnt
            }
        }
        val comparator = Comparator<Repository> { o1, o2 ->
            val o1CountRefs = pipelineRefCountMap[o1.repoHashId!!] ?: 0
            val o2CountRefs = pipelineRefCountMap[o2.repoHashId!!] ?: 0
            when {
                o1CountRefs < o2CountRefs -> 1
                o1CountRefs > o2CountRefs -> -1
                else -> when {
                    o1.enablePac == true -> -1
                    o2.enablePac == true -> 1
                    else -> 0
                }
            }
        }
        return repositories.sortedWith(comparator)
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
