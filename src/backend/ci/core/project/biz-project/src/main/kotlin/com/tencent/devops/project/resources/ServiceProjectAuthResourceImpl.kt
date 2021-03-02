package com.tencent.devops.project.resources

import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.service.ServiceProjectAuthResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceProjectAuthResourceImpl @Autowired constructor(
    val authProjectService: AuthProjectService
) : ServiceProjectAuthResource {
    override fun projectInfo(callBackInfo: CallbackRequestDTO): CallbackBaseResponseDTO? {
        val method = callBackInfo.method
        val page = callBackInfo.page
        when (method) {
            CallbackMethodEnum.LIST_INSTANCE -> {
                return authProjectService.getProjectList(page)
            }
            CallbackMethodEnum.FETCH_INSTANCE_INFO -> {
                val ids = callBackInfo.filter.idList.map { it.toString() }
                return authProjectService.getProjectInfo(ids)
            }
            CallbackMethodEnum.SEARCH_INSTANCE -> {
                return authProjectService.searchProjectInstances(callBackInfo.filter.keyword, page)
            }
        }
        return null
    }
}
