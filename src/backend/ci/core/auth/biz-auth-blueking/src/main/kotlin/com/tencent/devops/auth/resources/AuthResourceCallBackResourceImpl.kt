package com.tencent.devops.auth.resources

import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.devops.auth.api.AuthResourceCallBackResource
import com.tencent.devops.auth.pojo.BkResult
import com.tencent.devops.auth.pojo.dto.ResourceCallBackDTO
import com.tencent.devops.auth.pojo.vo.ResourceCallbackVo
import com.tencent.devops.auth.service.ResourceService
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AuthResourceCallBackResourceImpl @Autowired constructor(
    val resourceService: ResourceService
) : AuthResourceCallBackResource {
    override fun projectList(
        callBackInfo: CallbackRequestDTO,
        username: String?,
        token: String?
    ): CallbackBaseResponseDTO {
        return resourceService.getProjectList(
                page = callBackInfo.page,
                token = token ?: "",
                method = callBackInfo.method
            )

    }

    override fun resourceList(
        callBackInfo: CallbackRequestDTO,
        username: String?,
        token: String?
    ): CallbackBaseResponseDTO? {
        return resourceService.getResourceList(
                projectId = callBackInfo.filter!!.parent!!.id!!,
                actionType = callBackInfo.type,
                method = callBackInfo.method,
                page = callBackInfo.page,
                token = token ?: ""
            )
    }
}