package com.tencent.devops.process.engine.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.process.engine.dao.PipelineResDao
import com.tencent.devops.process.engine.dao.PipelineWebhookDao
import com.tencent.devops.process.engine.pojo.PipelineWebhook
import com.tencent.devops.process.service.scm.ScmService
import com.tencent.devops.process.util.WebhookRedisUtils
import com.tencent.devops.repository.api.ServiceRepositoryResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.NotFoundException

/**
 * 流水线webhook存储服务
 * @version 1.0
 */
@Service
class PipelineWebhookService @Autowired constructor(
    private val webhookRedisUtils: WebhookRedisUtils,
    private val scmService: ScmService,
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
        createPipelineFlag: Boolean ? = false
    ): Result<Boolean> {
        logger.info("save Webhook[$pipelineWebhook]")
        val repositoryConfig = getRepositoryConfig(pipelineWebhook, variables)

        var continueFlag = true
        if (createPipelineFlag != null && createPipelineFlag) {
            // 新增流水线时，模版里配置的代码库是变量或者当前项目下不存在，不需创建webhook
            try {
                scmService.getRepo(pipelineWebhook.projectId, repositoryConfig)
            } catch (e: Exception) {
                logger.info("skip save Webhook[$pipelineWebhook]: ${e.message}")
                continueFlag = false
            }
        }

        if (continueFlag) {
            val projectName = when (pipelineWebhook.repositoryType) {
                ScmType.CODE_GIT ->
                    scmService.addGitWebhook(pipelineWebhook.projectId, repositoryConfig, codeEventType)
                ScmType.CODE_SVN ->
                    scmService.addSvnWebhook(pipelineWebhook.projectId, repositoryConfig)
                ScmType.CODE_GITLAB ->
                    scmService.addGitlabWebhook(pipelineWebhook.projectId, repositoryConfig)
                ScmType.GITHUB -> {
                    val repo = client.get(ServiceRepositoryResource::class).get(pipelineWebhook.projectId, repositoryConfig.getURLEncodeRepositoryId(), repositoryConfig.repositoryType).data!!
                    repo.projectName
                }
                ScmType.CODE_TGIT -> {
                    // do nothing
                    null
                }
            }
            logger.info("add $projectName webhook to [$pipelineWebhook]")
            if (!projectName.isNullOrBlank()) {
                pipelineWebhookDao.save(dslContext, pipelineWebhook)
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

    fun deleteWebhook(pipelineId: String, userId: String): Result<Boolean> {
        logger.info("delete $pipelineId webhook by $userId")
        pipelineWebhookDao.delete(dslContext, pipelineId)
        return Result(true)
    }

    fun listRepositoryTypeWebhooks(repositoryType: ScmType, start: Int, limit: Int): Result<Collection<PipelineWebhook>> {
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

    private fun getExistWebhookPipelineByType(type: ScmType): HashMap<String, Set<String>> {
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
                        logger.error("repo[${it.repoHashId}] does not exist")
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
            ?: throw NotFoundException("流水线不存在")

        val triggerContainer = model.stages[0].containers[0] as TriggerContainer

        triggerContainer.params.forEach {
            result[it.id] = it.defaultValue.toString()
        }
        logger.info("[$pipelineId] Get the pipeline startup param - ($result)")
        return result
    }

    fun getModel(pipelineId: String, version: Int? = null): Model? {
        val modelString = pipelineResDao.getVersionModelString(dslContext, pipelineId, version)
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
        /*
        //如果项目名是三层的，比如svn+ssh://sh-svn.tencent.com/ied/ied_kihan_rep/server_proj，那对应的rep_name 是 ied_kihan_rep
        return if (second.endsWith("_proj")) {
            path.removePrefix("$domain/$first/$second").removePrefix("/")
        } else {
            path.removePrefix("$domain/$first/$second/$third").removePrefix("/")
        }
        */
    }

    fun getProjectName(projectName: String): String {
        // 如果项目名是三层的，比如ied/ied_kihan_rep/server_proj，那对应的rep_name 是 ied_kihan_rep
        val repoSplit = projectName.split("/")
        if (repoSplit.size != 3) {
            return projectName
        }
        return repoSplit[1].trim()
    }

    private fun getRepositoryConfig(pipelineWebhook: PipelineWebhook, variable: Map<String, String>? = null): RepositoryConfig {
        return when (pipelineWebhook.repoType) {
            RepositoryType.ID -> RepositoryConfig(pipelineWebhook.repoHashId, null, RepositoryType.ID)
            RepositoryType.NAME -> {
                val repoName = if (variable == null || variable.isEmpty()) {
                    pipelineWebhook.repoName!!
                } else {
                    EnvUtils.parseEnv(pipelineWebhook.repoName!!, variable)
                }
                RepositoryConfig(null, repoName, RepositoryType.NAME)
            }
            else -> {
                if (!pipelineWebhook.repoHashId.isNullOrBlank()) {
                    RepositoryConfig(pipelineWebhook.repoHashId, null, RepositoryType.ID)
                } else if (!pipelineWebhook.repoName.isNullOrBlank()) {
                    val repoName = if (variable == null || variable.isEmpty()) {
                        pipelineWebhook.repoName!!
                    } else {
                        EnvUtils.parseEnv(pipelineWebhook.repoName!!, variable)
                    }
                    RepositoryConfig(null, repoName, RepositoryType.NAME)
                } else {
                    // 两者不能同时为空
                    throw ParamBlankException("Webhook 的ID和名称同时为空")
                }
            }
        }
    }
}
