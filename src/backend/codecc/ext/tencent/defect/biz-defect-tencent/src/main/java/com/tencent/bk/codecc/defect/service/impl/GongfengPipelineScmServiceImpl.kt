package com.tencent.bk.codecc.defect.service.impl

import com.tencent.bk.codecc.defect.dao.mongotemplate.CodeRepoInfoDao
import com.tencent.bk.codecc.defect.service.PipelineScmService
import com.tencent.bk.codecc.task.api.ServiceGongfengTaskRestResource
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource
import com.tencent.devops.common.api.CodeRepoVO
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.pojo.CodeCCResult
import com.tencent.devops.common.api.util.JsonUtil
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
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.net.URLEncoder

@Service
@Primary
class GongfengPipelineScmServiceImpl @Autowired constructor(
        private val codeRepoInfoDao: CodeRepoInfoDao,
        private val client: Client
) : PipelineScmServiceImpl(codeRepoInfoDao, client), PipelineScmService {

    companion object {
        private val logger = LoggerFactory.getLogger(GongfengPipelineScmServiceImpl::class.java)
    }

    @Value("\${codecc.privatetoken:#{null}}")
    lateinit var codeccToken: String

    override fun getFileContent(
            taskId: Long, repoId: String?, filePath: String,
            reversion: String?, branch: String?, subModule: String?, createFrom: String
    ): String? {
        logger.info("start to get file content: $taskId, $repoId, $filePath, $reversion, $branch, $subModule, $createFrom")

        val fileContentResult = if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equals(createFrom, true)) {
            val repoUrl = client.get(ServiceGongfengTaskRestResource::class.java).getGongfengRepoUrl(taskId)
            logger.info("gongfeng project url is: ${repoUrl.data}")
            if (repoUrl.isNotOk() || repoUrl.data == null) {
                logger.error("get gongfeng repo url fail!")
                throw CodeCCException(CommonMessageCode.CODE_NORMAL_CONTENT_ERROR)
            }

            val fileContentResp: CodeCCResult<String>
            try {
                val result = client.getDevopsService(ExternalCodeccRepoResource::class.java).getGitFileContentCommon(
                        repoUrl = repoUrl.data!!,
                        filePath = filePath.removePrefix("/"),
                        ref = if(!reversion.isNullOrBlank()) reversion else branch,
                        //todo 要区分情景
                        token = codeccToken!!
                )
                fileContentResp = CodeCCResult(result.data ?: "")
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
            return super.getFileContent(taskId, repoId, filePath, reversion, branch, subModule, createFrom)
        }

        if (fileContentResult.isNotOk()) {
            logger.error("get file content fail!")
            throw CodeCCException(CommonMessageCode.CODE_NORMAL_CONTENT_ERROR)
        }
        return fileContentResult.data
    }
}