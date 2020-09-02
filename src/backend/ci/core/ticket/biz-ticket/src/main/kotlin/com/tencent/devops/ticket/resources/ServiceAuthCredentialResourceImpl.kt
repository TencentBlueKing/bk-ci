package com.tencent.devops.ticket.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.ticket.api.ServiceAuthCallbackResource
import com.tencent.devops.ticket.pojo.Cert
import com.tencent.devops.ticket.pojo.Credential
import com.tencent.devops.ticket.service.CertService
import com.tencent.devops.ticket.service.CredentialService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceAuthCredentialResourceImpl @Autowired constructor(
    private val credentialService: CredentialService,
    private val certService: CertService
) : ServiceAuthCallbackResource {

    override fun listCredential(projectId: String, page: Int?, pageSize: Int?): Result<Page<Credential>?> {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 20
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = credentialService.serviceList(projectId, limit.offset, limit.limit)
        return Result(Page(pageNotNull, pageSizeNotNull, result.count, result.records))
    }

    override fun listCert(projectId: String, page: Int?, pageSize: Int?): Result<Page<Cert>?> {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 20
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = certService.list(projectId, limit.offset, limit.limit)
        return Result(Page(pageNotNull, pageSizeNotNull, result.count, result.records))
    }

    override fun getCredentialInfos(credentialIds: Set<String>): Result<List<Credential>?> {
        return Result(credentialService.getCredentialByIds(null, credentialIds))
    }

    override fun getCertInfos(certIds: Set<String>): Result<List<Cert>?> {
        return Result(certService.getCertByIds(certIds))
    }
}