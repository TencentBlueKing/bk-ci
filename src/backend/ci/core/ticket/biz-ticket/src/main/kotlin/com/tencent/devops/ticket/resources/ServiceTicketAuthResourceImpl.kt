package com.tencent.devops.ticket.resources

import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.ticket.api.ServiceTicketAuthResource
import com.tencent.devops.ticket.service.AuthCertService
import com.tencent.devops.ticket.service.AuthCredentialService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceTicketAuthResourceImpl @Autowired constructor(
    val authCertService: AuthCertService,
    val authCredentialService: AuthCredentialService
) : ServiceTicketAuthResource {

    override fun certInfo(callBackInfo: CallbackRequestDTO, token: String): CallbackBaseResponseDTO? {
        val method = callBackInfo.method
        val page = callBackInfo.page
        val projectId = callBackInfo.filter.parent.id
        when (method) {
            CallbackMethodEnum.LIST_INSTANCE -> {
                return authCertService.getCert(projectId, page.offset.toInt(), page.limit.toInt(), token)
            }
            CallbackMethodEnum.FETCH_INSTANCE_INFO -> {
                val ids = callBackInfo.filter.idList.map { it.toString() }
                return authCertService.getCertInfo(ids, token)
            }
            CallbackMethodEnum.SEARCH_INSTANCE -> {
                return authCertService.searchCert(
                    projectId = projectId,
                    keyword = callBackInfo.filter.keyword,
                    limit = page.offset.toInt(),
                    offset = page.limit.toInt(),
                    token = token
                )
            }
        }
        return null
    }

    override fun credentialInfo(callBackInfo: CallbackRequestDTO, token: String): CallbackBaseResponseDTO? {
        val method = callBackInfo.method
        val page = callBackInfo.page
        val projectId = callBackInfo.filter.parent.id
        when (method) {
            CallbackMethodEnum.LIST_INSTANCE -> {
                return authCredentialService.getCredential(
                    projectId,
                    page.offset.toInt(),
                    page.limit.toInt(),
                    token
                )
            }
            CallbackMethodEnum.FETCH_INSTANCE_INFO -> {
                val ids = callBackInfo.filter.idList.map { it.toString() }
                return authCredentialService.getCredentialInfo(ids, token)
            }
            CallbackMethodEnum.SEARCH_INSTANCE -> {
                return authCredentialService.searchCredential(
                    projectId = projectId,
                    keyword = callBackInfo.filter.keyword,
                    limit = page.offset.toInt(),
                    offset = page.limit.toInt(),
                    token = token
                )
            }
        }
        return null
    }
}
