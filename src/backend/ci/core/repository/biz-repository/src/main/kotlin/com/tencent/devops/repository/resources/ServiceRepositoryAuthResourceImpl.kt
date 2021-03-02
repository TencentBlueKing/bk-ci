package com.tencent.devops.repository.resources

import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ServiceRepositoryAuthResource
import com.tencent.devops.repository.service.RepositoryAuthService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceRepositoryAuthResourceImpl @Autowired constructor(
    val repositoryAuthService: RepositoryAuthService
) : ServiceRepositoryAuthResource {

    override fun repositoryInfo(callBackInfo: CallbackRequestDTO): CallbackBaseResponseDTO? {
        val method = callBackInfo.method
        val page = callBackInfo.page
        val projectId = callBackInfo.filter.parent.id
        when (method) {
            CallbackMethodEnum.LIST_INSTANCE -> {
                return repositoryAuthService.getRepository(projectId, page.offset.toInt(), page.limit.toInt())
            }
            CallbackMethodEnum.FETCH_INSTANCE_INFO -> {
                val hashIds = callBackInfo.filter.idList.map { it.toString() }
                return repositoryAuthService.getRepositoryInfo(hashIds)
            }
            CallbackMethodEnum.SEARCH_INSTANCE -> {
                return repositoryAuthService.searchRepositoryInstances(
                    projectId = projectId,
                    keyword = callBackInfo.filter.keyword,
                    limit = page.offset.toInt(),
                    offset = page.limit.toInt())
            }
        }
        return null
    }
}
