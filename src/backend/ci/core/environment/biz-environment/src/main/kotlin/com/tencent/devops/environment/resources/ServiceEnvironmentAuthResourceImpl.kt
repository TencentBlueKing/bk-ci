package com.tencent.devops.environment.resources

import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.ServiceEnvironmentAuthResource
import com.tencent.devops.environment.service.AuthEnvService
import com.tencent.devops.environment.service.AuthNodeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceEnvironmentAuthResourceImpl @Autowired constructor(
    val authNodeService: AuthNodeService,
    val authEnvService: AuthEnvService
) : ServiceEnvironmentAuthResource {

    override fun environmentInfo(callBackInfo: CallbackRequestDTO): CallbackBaseResponseDTO? {
        val method = callBackInfo.method
        val page = callBackInfo.page
        val projectId = callBackInfo.filter.parent.id
        when (method) {
            CallbackMethodEnum.LIST_INSTANCE -> {
                return authEnvService.getEnv(projectId, page.offset.toInt(), page.limit.toInt())
            }
            CallbackMethodEnum.FETCH_INSTANCE_INFO -> {
                val ids = callBackInfo.filter.idList.map { it.toString() }
                return authEnvService.getEnvInfo(ids)
            }
            CallbackMethodEnum.SEARCH_INSTANCE -> {
                return authEnvService.searchEnv(
                    projectId = projectId,
                    keyword = callBackInfo.filter.keyword,
                    limit = page.offset.toInt(),
                    offset = page.limit.toInt())
            }
        }
        return null
    }

    override fun nodeInfo(callBackInfo: CallbackRequestDTO): CallbackBaseResponseDTO? {
        val method = callBackInfo.method
        val page = callBackInfo.page
        val projectId = callBackInfo.filter.parent.id
        when (method) {
            CallbackMethodEnum.LIST_INSTANCE -> {
                return authNodeService.getNode(projectId, page.offset.toInt(), page.limit.toInt())
            }
            CallbackMethodEnum.FETCH_INSTANCE_INFO -> {
                val ids = callBackInfo.filter.idList.map { it.toString() }
                return authNodeService.getNodeInfo(ids)
            }
            CallbackMethodEnum.SEARCH_INSTANCE -> {
                return authNodeService.searchNode(
                    projectId = projectId,
                    keyword = callBackInfo.filter.keyword,
                    limit = page.offset.toInt(),
                    offset = page.limit.toInt())
            }
        }
        return null
    }
}
