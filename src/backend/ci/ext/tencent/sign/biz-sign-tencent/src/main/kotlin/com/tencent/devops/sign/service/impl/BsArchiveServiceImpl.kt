package com.tencent.devops.sign.service.impl

import com.tencent.devops.common.archive.client.DirectBkRepoClient
import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.service.ArchiveService
import com.tencent.devops.sign.utils.IpaIconUtil
import com.tencent.devops.sign.utils.sha256
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class BsArchiveServiceImpl @Autowired constructor(
    private val directBkRepoClient: DirectBkRepoClient,
    private val commonConfig: CommonConfig,
    private val profile: Profile
) : ArchiveService {

    override fun archive(
        signedIpaFile: File,
        ipaSignInfo: IpaSignInfo,
        properties: MutableMap<String, String>?
    ): Boolean {
        logger.info("archive, signedIpaFile: ${signedIpaFile.absolutePath}, ipaSignInfo: $ipaSignInfo, properties: $properties")
        val path = if (ipaSignInfo.archiveType.toLowerCase() == "pipeline") {
            "${ipaSignInfo.pipelineId}/${ipaSignInfo.buildId}/${signedIpaFile.name}"
        } else {
            "${ipaSignInfo.archivePath}/${signedIpaFile.name}"
        }

        // icon图标
        try {
            if (null != properties) {
                val resolveIpaIcon = IpaIconUtil.resolveIpaIcon(signedIpaFile)
                if (null != resolveIpaIcon) {
                    val sha256 = resolveIpaIcon.inputStream().sha256()
                    val url = directBkRepoClient.uploadByteArray(
                        userId = ipaSignInfo.userId,
                        projectId = getIconProject(),
                        repoName = getIconRepo(),
                        path = "/app-icon/ipa/$sha256.png",
                        byteArray = resolveIpaIcon
                    )
                    properties["appIcon"] = url
                }
            }
        } catch (ignored: Exception) {
        }

        directBkRepoClient.uploadLocalFile(
            userId = ipaSignInfo.userId,
            projectId = ipaSignInfo.projectId,
            repoName = ipaSignInfo.archiveType.toLowerCase(),
            path = path,
            file = signedIpaFile,
            metadata = properties ?: mapOf(),
            override = true
        )
        return true
    }

    private fun getIconProject(): String {
        return if (profile.isDev()) {
            "repo-dev-test"
        } else {
            "bkdevops"
        }
    }

    private fun getIconRepo(): String {
        return if (profile.isDev()) {
            "public"
        } else {
            "app-icon"
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BsArchiveServiceImpl::class.java)

        private const val BKREPO_OVERRIDE = "X-BKREPO-OVERWRITE"
        private const val BKREPO_UID = "X-BKREPO-UID"
    }
}
