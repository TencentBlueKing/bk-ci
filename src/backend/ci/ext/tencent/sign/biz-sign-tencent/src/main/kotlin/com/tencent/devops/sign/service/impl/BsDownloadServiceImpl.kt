package com.tencent.devops.sign.service.impl

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryDownLoadResource
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.service.config.CommonConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.pojo.enums.GatewayType
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.dao.SignHistoryDao
import com.tencent.devops.sign.dao.SignIpaInfoDao
import com.tencent.devops.sign.service.DownloadService
import org.jooq.DSLContext

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
        var artifactoryType: ArtifactoryType? = null
        var path: String? = null
        when (signIpaInfoResult.archiveType.toLowerCase()) {
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
        var downlouadUserId = when (downloadType) {
            "service", "build" -> {
                signIpaInfoResult.userId
            }
            "user" -> {
                userId
            }
            else -> {
                userId
            }
        }
        val downloadUrl = client.getGateway(ServiceArtifactoryDownLoadResource::class, GatewayType.DEVNET_PROXY).downloadUrl(
            projectId = signIpaInfoResult.projectId,
            artifactoryType = artifactoryType,
            userId = downlouadUserId,
            path = path,
            ttl = 7200,
            directed = true

        ).data?.url
//        val downloadUrl = when (downloadType) {
//            "service","build" -> {
//                client.getGateway(ServiceArtifactoryDownLoadResource::class, GatewayType.DEVNET_PROXY).downloadUrl(
//                        projectId = signIpaInfoResult.projectId,
//                        artifactoryType = artifactoryType,
//                        userId = signIpaInfoResult.userId,
//                        path = path,
//                        ttl = 7200,
//                        directed = true
//
//                ).data?.url
//            }
//            "user" -> {
//                client.getGateway(UserArtifactoryResource::class, GatewayType.DEVNET_PROXY).downloadUrl(
//                        userId = userId,
//                        projectId = signIpaInfoResult.projectId,
//                        artifactoryType = artifactoryType,
//                        path = path
//                ).data?.url
//            }
//            else -> {
//                client.getGateway(UserArtifactoryResource::class, GatewayType.DEVNET_PROXY).downloadUrl(
//                        userId = userId,
//                        projectId = signIpaInfoResult.projectId,
//                        artifactoryType = artifactoryType,
//                        path = path
//                ).data?.url
//            }
//        }
        if (downloadUrl == null) {
            logger.error("创建下载连接失败(resignId=$resignId)")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_CREATE_DOWNLOAD_URL, defaultMessage = "创建下载连接失败。")
        }
        return downloadUrl
    }
}