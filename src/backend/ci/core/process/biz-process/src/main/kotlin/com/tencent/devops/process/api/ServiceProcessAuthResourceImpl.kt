package com.tencent.devops.process.api

import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.auth.ServiceProcessAuthResource
import com.tencent.devops.process.service.AuthPipelineService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceProcessAuthResourceImpl @Autowired constructor(
    val authPipelineService: AuthPipelineService
) : ServiceProcessAuthResource {

    override fun pipelineInfo(callBackInfo: CallbackRequestDTO): CallbackBaseResponseDTO? {
        val method = callBackInfo.method
        val page = callBackInfo.page
        val projectId = callBackInfo.filter.parent.id
        when (method) {
            CallbackMethodEnum.LIST_INSTANCE -> {
                return authPipelineService.getPipeline(projectId, page.offset.toInt(), page.limit.toInt())
            }
            CallbackMethodEnum.FETCH_INSTANCE_INFO -> {
                val ids = callBackInfo.filter.idList.map { it.toString() }
                return authPipelineService.getPipelineInfo(ids)
            }
            CallbackMethodEnum.SEARCH_INSTANCE -> {
                return authPipelineService.searchPipeline(
                    projectId = projectId,
                    keyword = callBackInfo.filter.keyword,
                    limit = page.offset.toInt(),
                    offset = page.limit.toInt())
            }
        }
        return null
    }
}
