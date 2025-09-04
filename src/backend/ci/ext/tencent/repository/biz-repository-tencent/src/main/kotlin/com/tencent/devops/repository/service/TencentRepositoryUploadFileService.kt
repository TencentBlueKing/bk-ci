package com.tencent.devops.repository.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.CommonServiceUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.support.api.service.ServiceFileResource
import java.io.File
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Primary
@Service
class TencentRepositoryUploadFileService @Autowired constructor(
    private val client: Client
) : RepositoryUploadFileService {

    override fun uploadFile(userId: String, file: File, filePath: String): String {
        val serviceUrlPrefix = client.getServiceUrl(ServiceFileResource::class)
        var serviceUrl = "$serviceUrlPrefix/service/file/upload?userId=$userId&fileRepoPath=$filePath"
        CommonServiceUtils.uploadFileToService(serviceUrl, file).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn("$userId upload file:${file.name} fail,responseContent:$responseContent")
                throw RemoteServiceException(
                    errorMessage = I18nUtil.getCodeLanMessage(
                        messageCode = CommonMessageCode.SYSTEM_ERROR,
                        language = I18nUtil.getLanguage(userId)
                    ),
                    errorCode = response.code
                )
            }
            val urlResult = JsonUtil.to(responseContent, object : TypeReference<Result<String?>>() {})
            return urlResult.data ?: ""
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TencentRepositoryUploadFileService::class.java)
    }
}
