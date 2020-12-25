package com.tencent.devops.auth.resources

import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.devops.auth.api.AuthResourceCallBackResource
import com.tencent.devops.auth.service.ResourceService
import com.tencent.devops.common.web.RestResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AuthResourceCallBackResourceImpl @Autowired constructor(
    val resourceService: ResourceService
) : AuthResourceCallBackResource {
    override fun projectInfo(
        callBackInfo: CallbackRequestDTO,
        token: String
    ): CallbackBaseResponseDTO {
        return resourceService.getProjectInfo(
                callBackInfo = callBackInfo,
                method = callBackInfo.method,
                token = token
            )
    }

    override fun resourceList(
        callBackInfo: CallbackRequestDTO,
        token: String
    ): CallbackBaseResponseDTO? {
        logger.info("resourceList: $callBackInfo, token: $token")
        return resourceService.getResource(
                input = callBackInfo,
                token = token
            )
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}