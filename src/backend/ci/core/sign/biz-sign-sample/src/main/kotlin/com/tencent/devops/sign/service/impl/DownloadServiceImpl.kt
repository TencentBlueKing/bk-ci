package com.tencent.devops.sign.service.impl

import com.google.gson.JsonParser
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PIPELINE_ID
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.service.ArchiveService
import com.tencent.devops.sign.service.SignService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.dao.SignHistoryDao
import com.tencent.devops.sign.dao.SignIpaInfoDao
import com.tencent.devops.sign.service.DownloadService
import okhttp3.*
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

    override fun getDownloadUrl(userId: String?, resignId: String, downloadType: String): String {
        val signIpaInfoResult = signIpaInfoDao.getSignInfo(dslContext, resignId)
        if(signIpaInfoResult == null) {
            logger.error("签名任务签名信息(resignId=$resignId)不存在。")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_RESIGN_TASK_NOT_EXIST, defaultMessage = "签名任务不存在。")
        }
        val signHistoryResult = signHistoryDao.getSignHistory(dslContext, resignId)
        if(signHistoryResult == null) {
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
            else -> "user"
        }

        return "${commonConfig.devopsHostGateway}/artifactory/api/$downloadTypePath/artifactories/file/download/local?filePath=${URLEncoder.encode(filePath, "UTF-8")}"

    }

}