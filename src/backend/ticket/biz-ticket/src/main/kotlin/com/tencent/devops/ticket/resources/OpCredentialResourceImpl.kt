package com.tencent.devops.ticket.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.ticket.api.OpCredentialResource
import com.tencent.devops.ticket.pojo.CredentialWithPermission
import com.tencent.devops.ticket.pojo.enums.CredentialType
import com.tencent.devops.ticket.service.CredentialService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpCredentialResourceImpl @Autowired constructor(
    private val credentialService: CredentialService
) : OpCredentialResource {

    override fun list(
        userId: String,
        projectId: String,
        credentialTypesString: String?,
        page: Int?,
        pageSize: Int?,
        keyword: String?
    ): Result<Page<CredentialWithPermission>> {
        val credentialTypes = credentialTypesString?.split(",")?.map {
            CredentialType.valueOf(it)
        }
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 20
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = credentialService.userList(userId, projectId, credentialTypes, limit.offset, limit.limit, keyword)
        return Result(Page(pageNotNull, pageSizeNotNull, result.count, result.records))
    }
}