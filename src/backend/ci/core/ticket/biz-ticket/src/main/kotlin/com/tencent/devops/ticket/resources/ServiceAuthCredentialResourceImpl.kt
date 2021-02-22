/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.ticket.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.ticket.api.ServiceAuthCallbackResource
import com.tencent.devops.ticket.pojo.Cert
import com.tencent.devops.ticket.pojo.Credential
import com.tencent.devops.ticket.service.CertService
import com.tencent.devops.ticket.service.CredentialService
import org.springframework.beans.factory.annotation.Autowired

@Suppress("ALL")
@RestResource
class ServiceAuthCredentialResourceImpl @Autowired constructor(
    private val credentialService: CredentialService,
    private val certService: CertService
) : ServiceAuthCallbackResource {

    override fun listCredential(projectId: String, offset: Int?, limit: Int?): Result<Page<Credential>?> {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        val result = credentialService.serviceList(projectId, offset!!, limit!!)
        return Result(Page(offset!!, limit!!, result.count, result.records))
    }

    override fun listCert(projectId: String, offset: Int?, limit: Int?): Result<Page<Cert>?> {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        val result = certService.list(projectId, offset!!, limit!!)
        return Result(Page(offset!!, limit!!, result.count, result.records))
    }

    override fun getCredentialInfos(credentialIds: Set<String>): Result<List<Credential>?> {
        return Result(credentialService.getCredentialByIds(null, credentialIds))
    }

    override fun getCertInfos(certIds: Set<String>): Result<List<Cert>?> {
        return Result(certService.getCertByIds(certIds))
    }

    override fun searchCredentialById(
        projectId: String,
        offset: Int?,
        limit: Int?,
        credentialId: String
    ): Result<Page<Credential>?> {
        throw NotImplementedError("An operation is not implemented: Not yet implemented")
    }

    override fun searchCertById(projectId: String, offset: Int?, limit: Int?, certId: String): Result<Page<Cert>?> {
        throw NotImplementedError("An operation is not implemented: Not yet implemented")
    }
}
