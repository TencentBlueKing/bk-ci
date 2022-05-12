package com.tencent.bk.codecc.defect.service.impl

import com.tencent.bk.codecc.defect.dao.mongotemplate.CodeRepoInfoDao
import com.tencent.bk.codecc.defect.service.PipelineScmService
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource
import com.tencent.devops.common.api.CodeRepoVO
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.codecc.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.util.HttpPathUrlUtil
import com.tencent.devops.repository.api.ExternalCodeccRepoResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.api.scm.ServiceGitResource
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang.math.NumberUtils
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLEncoder

@Service
class PipelineScmServiceImpl @Autowired constructor(
        private val codeRepoInfoDao: CodeRepoInfoDao,
        private val client: Client
) : PipelineScmService {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineScmServiceImpl::class.java)
    }

    @Value("\${codecc.privatetoken:#{null}}")
    lateinit var codeccToken: String

    @Value("\${bkci.public.url:#{null}}")
    lateinit var devopsHost: String

    @Value("\${codecc.public.url:#{null}}")
    lateinit var codeccHost: String

    override fun getFileContent(
            taskId: Long, repoId: String?, filePath: String,
            reversion: String?, branch: String?, subModule: String?, createFrom: String
    ): String? {
        logger.info("start to get file content: $taskId, $repoId, $filePath, $reversion, $branch, $subModule, $createFrom")

        val fileContentResult = if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equals(createFrom, true)) {
            val repoUrl = client.get(ServiceTaskRestResource::class.java).getGongfengRepoUrl(taskId)
            logger.info("gongfeng project url is: ${repoUrl.data}")
            if (repoUrl.isNotOk() || repoUrl.data == null) {
                logger.error("get gongfeng repo url fail!")
                throw CodeCCException(CommonMessageCode.CODE_NORMAL_CONTENT_ERROR)
            }

            val fileContentResp: Result<String>
            try {
                fileContentResp = client.getDevopsService(ExternalCodeccRepoResource::class.java).getGitFileContentCommon(
                        repoUrl = repoUrl.data!!,
                        filePath = filePath.removePrefix("/"),
                        ref = if(!reversion.isNullOrBlank()) reversion else branch,
                        //todo 要区分情景
                        token = codeccToken!!
                )
            } catch (e: Exception) {
                logger.error("get git file content fail!, repoUrl: {}, filePath: {}, token: {}", repoUrl.data!!, filePath, codeccToken, e)
                throw CodeCCException(CommonMessageCode.CODE_CONTENT_ERROR)
            }
            if (fileContentResp.isNotOk()) {
                logger.info("get git file content fail!, repoUrl: {}, filePath: {}, token: {}", repoUrl.data!!, filePath, codeccToken)
                throw CodeCCException(CommonMessageCode.CODE_CONTENT_ERROR)
            }
            fileContentResp
        } else {
            if (reversion.isNullOrBlank()) {
                return null
            }
            var response = doGetFileContentV2(repoId!!, filePath, reversion, branch, subModule)

            // svn路径变更可能拿不到文件内容,需要用最新的reversion
            if (response.data.isNullOrBlank() && branch.isNullOrBlank() && NumberUtils.isNumber(reversion)) {
                response = doGetFileContentV2(repoId, filePath, "-1", branch, subModule)
            }

            // svn路径带域名的需要特殊处理
            if (response.data.isNullOrBlank() && branch.isNullOrBlank() && NumberUtils.isNumber(reversion)) {
                // 倒叙遍历，去掉路径带域名前面的部分
                val reversedFilePathList = mutableListOf<String>()
                run breaking@{
                    filePath.split("/").reversed().forEach {
                        // 路径包含域名则退出
                        if (it.endsWith(".com")) {
                            return@breaking
                        }
                        reversedFilePathList.add(it)
                    }
                }
                val noHostPath = reversedFilePathList.reversed().joinToString("/")
                response = doGetFileContent(repoId, noHostPath, reversion, branch, subModule)
            }

            response
        }

        if (fileContentResult.isNotOk()) {
            logger.error("get file content fail!")
            throw CodeCCException(CommonMessageCode.CODE_NORMAL_CONTENT_ERROR)
        }
        return fileContentResult.data
    }

    override fun getFileContentOauth(userId: String, repoName: String, filePath: String, ref: String?): String? {
        return try {
            logger.info("start to get file content oauth: $userId, $repoName, $filePath, $ref")
            val result = client.getDevopsService(ExternalCodeccRepoResource::class.java)
                    .getGitFileContentOAuth(userId, repoName, filePath, ref)
            if (result.status.toString() == com.tencent.devops.common.api.constant.CommonMessageCode.OAUTH_TOKEN_IS_INVALID) {
                throw CodeCCException(errorCode = CommonMessageCode.OAUTH_TOKEN_IS_INVALID)
            }
            result.data
        } catch (e: CodeCCException) {
            if (e.errorCode == CommonMessageCode.OAUTH_TOKEN_IS_INVALID) throw e
            else return ""
        } catch (e: Throwable) {
            ""
        }
    }

    private fun doGetFileContent(repoId: String, filePath: String,
                                   reversion: String?, branch: String?, subModule: String?): Result<String> {
        try {
            return client.getDevopsService(ExternalCodeccRepoResource::class.java).getFileContent(
                repoId = repoId,
                filePath = filePath.removePrefix("/"),
                reversion = reversion,
                branch = branch,
                subModule = subModule ?: "",
                repositoryType = RepositoryType.ID
            )
        } catch (e: Exception) {
            logger.error("get file content v2 fail!, repoId: {}, filePath: {}, reversion: {}, branch: {}, subModule: {}, ", repoId, filePath, reversion,
                branch, subModule ?: "", e)
        }
        return Result("")
    }

    private fun doGetFileContentV2(repoId: String, filePath: String,
                                   reversion: String?, branch: String?, subModule: String?): Result<String> {
        try {
            return client.getDevopsService(ExternalCodeccRepoResource::class.java).getFileContentV2(
                    repoId = repoId,
                    filePath = filePath.removePrefix("/"),
                    reversion = reversion,
                    branch = branch,
                    subModule = subModule ?: "",
                    repositoryType = RepositoryType.ID
            )
        } catch (e: Exception) {
            logger.error("get file content v2 fail!, repoId: {}, filePath: {}, reversion: {}, branch: {}, subModule: {}, ", repoId, filePath, reversion,
                    branch, subModule ?: "", e)
        }
        return Result("")
    }

    override fun getCodeRepoListByTaskIds(taskIds: Set<Long>, projectId: String): Map<Long, Set<CodeRepoVO>> {
        val codeRepoInfoEntities = codeRepoInfoDao.findFirstByTaskIdOrderByCreatedDate(taskIds)
        val repoResult = client.getDevopsService(ServiceRepositoryResource::class.java).listByProjects(setOf(projectId), 1, 20000)
        val repoList = if (repoResult.isNotOk()) listOf() else repoResult.data?.records ?: listOf()
        val repoMap = repoList.associate { it.repositoryHashId to it.aliasName }
        return if (CollectionUtils.isEmpty(codeRepoInfoEntities)) {
            mapOf()
        } else codeRepoInfoEntities.associate {
            it.taskId to if (it.repoList.isEmpty()) setOf() else it.repoList.map { codeRepoEntity ->
                with(codeRepoEntity) {
                    CodeRepoVO(repoId, revision, branch, repoMap[repoId], null)
                }
            }.toSet()
        }
    }

    override fun getOauthUrl(userId: String, projectId: String, taskId: Long, toolName: String): String {
        val createFrom = client.get(ServiceTaskRestResource::class.java).getTaskInfoById(taskId).data?.createFrom
        logger.info("Oauth get task create from, {} {}", createFrom, codeccHost)
        val redirectUrl = if (createFrom == ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value()) {
            HttpPathUrlUtil.getCodeccTargetUrl(codeccHost, projectId, taskId)
        } else {
            HttpPathUrlUtil.getTargetUrl(devopsHost, projectId, taskId, toolName)
        }

        val authParams = mapOf(
                "projectId" to projectId,
                "userId" to userId,
                "randomStr" to "BK_DEVOPS__${RandomStringUtils.randomAlphanumeric(8)}",
                "redirectUrlType" to "spec",
                "redirectUrl" to redirectUrl
        )
        val authParamJsonStr = URLEncoder.encode(JsonUtil.toJson(authParams), "UTF-8")
        logger.info("getAuthUrl authParamJsonStr is: $authParamJsonStr")
        return client.getDevopsService(ServiceGitResource::class.java).getAuthUrl(authParamJsonStr = authParamJsonStr).data
                ?: ""
    }
}