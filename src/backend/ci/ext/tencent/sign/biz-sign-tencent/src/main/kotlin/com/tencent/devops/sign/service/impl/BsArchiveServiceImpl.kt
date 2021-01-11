package com.tencent.devops.sign.service.impl

import com.tencent.bkrepo.common.api.constant.AUTH_HEADER_UID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.archive.client.DirectBkRepoClient
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.service.ArchiveService
import com.tencent.devops.sign.utils.IpaIconUtil
import com.tencent.devops.sign.utils.sha256
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class BsArchiveServiceImpl @Autowired constructor(
    private val directBkRepoClient: DirectBkRepoClient,
    private val commonConfig: CommonConfig
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
        if (null != properties) {
            val resolveIpaIcon = IpaIconUtil.resolveIpaIcon(signedIpaFile)
            if (null != resolveIpaIcon) {
                val iconPath = uploadIconFile(resolveIpaIcon, "ipa", ipaSignInfo)
                properties["appIcon"] = "http://bkrepo.oa.com/generic/$iconPath"
            }
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

    private fun uploadIconFile(iconContent: ByteArray, appTypeStr: String, ipaSignInfo: IpaSignInfo): String? {
        try {
            val sha256 = iconContent.inputStream().sha256()
            val iconPath = "$ICON_PROJECT/$ICON_REPO/app-icon/$appTypeStr/$sha256.png"
            val request = buildAtomPut(
                "/bkrepo/api/service/generic/$iconPath",
                RequestBody.create(MediaType.parse("application/octet-stream"), iconContent),
                mutableMapOf(BKREPO_OVERRIDE to "true", BKREPO_UID to "app-icon"),
                ipaSignInfo
            )
            val response = OkhttpUtils.doHttp(request)
            if (!response.isSuccessful) {
                logger.warn("upload icon is not successful , response:{}", response.body()!!.toString())
            }
            return iconPath
        } catch (e: Exception) {
            logger.warn("upload icon file error: ${e.message}")
        }
        return null
    }

    private fun buildAtomPut(
        path: String,
        requestBody: RequestBody,
        headers: MutableMap<String, String>,
        ipaSignInfo: IpaSignInfo
    ): Request {
        val url = commonConfig.devopsDevnetProxyGateway + path
        headers[AUTH_HEADER_UID] = ipaSignInfo.userId
        headers[AUTH_HEADER_DEVOPS_PROJECT_ID] = ipaSignInfo.projectId

        return Request.Builder().url(url).headers(Headers.of(headers)).put(requestBody).build()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BsArchiveServiceImpl::class.java)

        private const val ICON_PROJECT = "bkdevops"
        private const val ICON_REPO = "app-icon"
        private const val BKREPO_OVERRIDE = "X-BKREPO-OVERWRITE"
        private const val BKREPO_UID = "X-BKREPO-UID"
    }
}