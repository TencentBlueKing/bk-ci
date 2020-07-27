package com.tencent.devops.sign.service.impl

import com.google.gson.JsonParser
import com.tencent.devops.artifactory.api.service.ServiceArtifactoryDownLoadResource
import com.tencent.devops.artifactory.api.user.UserArtifactoryResource
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
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
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.pojo.enums.GatewayType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.dao.SignHistoryDao
import com.tencent.devops.sign.dao.SignIpaInfoDao
import com.tencent.devops.sign.service.DownloadService
import okhttp3.*
import org.jooq.DSLContext
import java.net.URLEncoder

@Service
class BsDownloadServiceImpl @Autowired constructor(
        private val dslContext: DSLContext,
        private val signIpaInfoDao: SignIpaInfoDao,
        private val signHistoryDao: SignHistoryDao,
        private val commonConfig: CommonConfig,
        private val client: Client
) : DownloadService {

    companion object {
        private val logger = LoggerFactory.getLogger(BsDownloadServiceImpl::class.java)
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
        var artifactoryType: ArtifactoryType? = null
        var path: String? = null
        when(signIpaInfoResult.archiveType.toLowerCase()) {
            "pipeline" -> {
                artifactoryType = ArtifactoryType.PIPELINE
                path = "/${signIpaInfoResult.pipelineId}/${signIpaInfoResult.buildId}/${signHistoryResult.resultFileName}"
            }
            "custom" -> {
                artifactoryType = ArtifactoryType.CUSTOM_DIR
                path = "/${signIpaInfoResult.archivePath?.trim('/')}/${signHistoryResult.resultFileName}"
            }
            else -> {
                artifactoryType = ArtifactoryType.PIPELINE
                path = "/${signIpaInfoResult.pipelineId}/${signIpaInfoResult.buildId}/${signHistoryResult.resultFileName}"
            }
        }
        val downloadUrl = when (downloadType) {
            "user" -> {
                client.getGateway(ServiceArtifactoryDownLoadResource::class, GatewayType.DEVNET_PROXY).downloadUrl(
                        projectId = signIpaInfoResult.projectId,
                        artifactoryType = artifactoryType,
                        userId = userId?:"",
                        path = path,
                        channelCode = ChannelCode.BS

                ).data?.url
            }
            "service" -> {
                client.getGateway(UserArtifactoryResource::class, GatewayType.DEVNET_PROXY).downloadUrl(
                        projectId = signIpaInfoResult.projectId,
                        artifactoryType = artifactoryType,
                        userId = userId?:"",
                        path = path,
                        channelCode = ChannelCode.BS

                ).data?.url
            }
            else -> "user"
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
        return


        return "${commonConfig.devopsHostGateway}/artifactory/api/$downloadTypePath/artifactories/file/download/local?filePath=${URLEncoder.encode(filePath, "UTF-8")}"
    }

}