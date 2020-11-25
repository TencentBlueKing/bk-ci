package com.tencent.devops.sign.service.impl

import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.client.JfrogClient
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.gray.RepoGray
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.service.ArchiveService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class BsArchiveServiceImpl @Autowired constructor(
    private val bkRepoClient: BkRepoClient,
    private val commonConfig: CommonConfig,
    private val repoGray: RepoGray,
    private val redisOperation: RedisOperation
) : ArchiveService {

    companion object {
        private val logger = LoggerFactory.getLogger(BsArchiveServiceImpl::class.java)
        private const val METADATA_PREFIX = "X-BKREPO-META-"
        private const val BK_REPO_CUSTOM = "X-BKREPO-OVERWRITE"
    }

    override fun archive(
        signedIpaFile: File,
        ipaSignInfo: IpaSignInfo,
        properties: Map<String, String>?
    ): Boolean {
        val isRepoGray = repoGray.isGray(ipaSignInfo.projectId, redisOperation)
        val path = if (ipaSignInfo.archiveType.toLowerCase() == "pipeline") {
            "${ipaSignInfo.pipelineId}/${ipaSignInfo.buildId}/${signedIpaFile.name}"
        } else {
            "${ipaSignInfo.archivePath}/${signedIpaFile.name}"
        }
        if (isRepoGray) {
            bkRepoClient.uploadLocalFile(
                    userId = ipaSignInfo.userId,
                    projectId = ipaSignInfo.projectId,
                    repoName = ipaSignInfo.archiveType.toLowerCase(),
                    path = path,
                    file = signedIpaFile,
                    gatewayFlag = true,
                    bkrepoApiUrl = null,
                    userName = null,
                    password = null,
                    properties = properties,
                    gatewayUrl = commonConfig.devopsDevnetProxyGateway!!

            )
        } else {
            val jfrogClient = JfrogClient(
                    gatewayUrl = commonConfig.devopsDevnetProxyGateway!!,
                    projectId = ipaSignInfo.projectId,
                    pipelineId = ipaSignInfo.pipelineId ?: "",
                    buildId = ipaSignInfo.buildId ?: ""
            )
            jfrogClient.uploadFile(
                    userId = ipaSignInfo.userId,
                    repoName = ipaSignInfo.archiveType,
                    path = path,
                    file = signedIpaFile,
                    properties = properties
            )
        }

        return true
    }
}