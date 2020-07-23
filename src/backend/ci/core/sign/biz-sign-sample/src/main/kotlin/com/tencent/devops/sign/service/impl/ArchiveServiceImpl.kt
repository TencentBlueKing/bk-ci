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
import okhttp3.*

@Service
class ArchiveServiceImpl @Autowired constructor(
        private val commonConfig: CommonConfig
) : ArchiveService {

    companion object {
        private val logger = LoggerFactory.getLogger(ArchiveServiceImpl::class.java)
    }

    override fun archive(
            signedIpaFile: File,
            ipaSignInfo: IpaSignInfo,
            properties: Map<String, String>? = null
    ): Boolean {
        logger.info("uploadFile, userId: ${ipaSignInfo.userId}, projectId: ${ipaSignInfo.projectId},archiveType: ${ipaSignInfo.archiveType}, archivePath: ${ipaSignInfo.archivePath}")
        val artifactoryType = when (ipaSignInfo.archiveType.toLowerCase()) {
            "pipeline" -> FileTypeEnum.BK_ARCHIVE
            "custom" -> FileTypeEnum.BK_CUSTOM
            else -> FileTypeEnum.BK_ARCHIVE
        }
        val url =
                "${commonConfig.devopsDevnetProxyGateway}/ms/artifactory/api/service/artifactories/file/archive?fileType=$artifactoryType&customFilePath=${ipaSignInfo.archivePath}"
        val fileBody = RequestBody.create(MediaType.parse("multipart/form-data"), signedIpaFile)
        val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", signedIpaFile.name, fileBody)
                .build()
        val headers = mutableMapOf<String, String>()
        headers[AUTH_HEADER_DEVOPS_PROJECT_ID] = ipaSignInfo.projectId
        headers[AUTH_HEADER_DEVOPS_PIPELINE_ID] = ipaSignInfo.pipelineId ?: ""
        headers[AUTH_HEADER_DEVOPS_BUILD_ID] = ipaSignInfo.buildId ?: ""
        val request = Request.Builder()
                .url(url)
                .headers(Headers.of(headers))
                .post(requestBody)
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("artifactory upload file failed. url:$url. response:$responseContent")
                throw RemoteServiceException("artifactory upload file failed. url:$url. response:$responseContent")
            }
        }
        return true
    }
}