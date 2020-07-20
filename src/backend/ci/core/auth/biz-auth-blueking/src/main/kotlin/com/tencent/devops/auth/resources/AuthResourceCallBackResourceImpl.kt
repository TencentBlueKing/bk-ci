package com.tencent.devops.auth.resources

import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.devops.auth.api.AuthResourceCallBackResource
import com.tencent.devops.auth.service.ResourceService
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AuthResourceCallBackResourceImpl @Autowired constructor(
    val resourceService: ResourceService
) : AuthResourceCallBackResource {
    override fun projectList(
        callBackInfo: CallbackRequestDTO,
        token: String
    ): CallbackBaseResponseDTO {
        return resourceService.getProjectList(
                page = callBackInfo.page,
                method = callBackInfo.method,
                token = token
            )

    }

    override fun resourceList(
        callBackInfo: CallbackRequestDTO,
        token: String
    ): CallbackBaseResponseDTO? {
        return resourceService.getResourceList(
                projectId = callBackInfo.filter!!.parent!!.id!!,
                actionType = callBackInfo.type,
                method = callBackInfo.method,
                page = callBackInfo.page,
                token = token
            )
    }
}