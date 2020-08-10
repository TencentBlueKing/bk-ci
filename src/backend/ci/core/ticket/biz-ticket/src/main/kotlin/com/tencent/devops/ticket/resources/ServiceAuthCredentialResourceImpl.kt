package com.tencent.devops.ticket.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.ticket.api.ServiceAuthCredentialResource
import com.tencent.devops.ticket.pojo.Credential
import com.tencent.devops.ticket.service.CredentialService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceAuthCredentialResourceImpl @Autowired constructor(
    private val credentialService: CredentialService
) : ServiceAuthCredentialResource {

    override fun list(projectId: String, page: Int?, pageSize: Int?): Result<Page<Credential>> {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 20
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = credentialService.serviceList(projectId, limit.offset, limit.limit)
        return Result(Page(pageNotNull, pageSizeNotNull, result.count, result.records))
    }
}