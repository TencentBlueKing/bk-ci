package com.tencent.devops.sign.service.impl

import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.common.service.config.CommonConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.dao.SignHistoryDao
import com.tencent.devops.sign.dao.SignIpaInfoDao
import com.tencent.devops.sign.service.DownloadService
import org.jooq.DSLContext
import java.net.URLEncoder

@Service
class DownloadServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val signIpaInfoDao: SignIpaInfoDao,
    private val signHistoryDao: SignHistoryDao,
    private val commonConfig: CommonConfig
) : DownloadService {

    companion object {
        private val logger = LoggerFactory.getLogger(DownloadServiceImpl::class.java)
    }

    override fun getDownloadUrl(userId: String, resignId: String, downloadType: String): String {
        val signIpaInfoResult = signIpaInfoDao.getSignInfo(dslContext, resignId)
        if (signIpaInfoResult == null) {
            logger.error("签名任务签名信息(resignId=$resignId)不存在。")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_RESIGN_TASK_NOT_EXIST, defaultMessage = "签名任务不存在。")
        }
        val signHistoryResult = signHistoryDao.getSignHistory(dslContext, resignId)
        if (signHistoryResult == null) {
            logger.error("签名任务签名历史(resignId=$resignId)不存在。")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_RESIGN_TASK_NOT_EXIST, defaultMessage = "签名任务不存在。")
        }
        val filePath = when (signIpaInfoResult.archiveType.toLowerCase()) {
            "pipeline" -> {
                "${FileTypeEnum.BK_ARCHIVE.fileType}/${signIpaInfoResult.projectId}/${signIpaInfoResult.pipelineId}/${signIpaInfoResult.buildId}/${signHistoryResult.resultFileName ?: "result.ipa"}"
            }
            "custom" -> {
                "${FileTypeEnum.BK_CUSTOM.fileType}/${signIpaInfoResult.projectId}/${signIpaInfoResult.archivePath?.trim('/')}/${signHistoryResult.resultFileName ?: "result.ipa"}"
            }
            else -> {
                // 默认是流水线
                "${FileTypeEnum.BK_ARCHIVE.fileType}/${signIpaInfoResult.projectId}/${signIpaInfoResult.pipelineId}/${signIpaInfoResult.buildId}/${signHistoryResult.resultFileName ?: "result.ipa"}"
            }
        }
        val downloadTypePath = when (downloadType) {
            "user" -> "user"
            "service" -> "service"
            "build" -> "build"
            else -> "user"
        }

        return "${commonConfig.devopsHostGateway}/artifactory/api/$downloadTypePath/artifactories/file/download/local?filePath=${URLEncoder.encode(filePath, "UTF-8")}"
    }
}