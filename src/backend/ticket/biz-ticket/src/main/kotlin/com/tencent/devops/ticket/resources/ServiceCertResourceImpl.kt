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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
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
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.BkAuthPermission
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.ticket.api.ServiceCertResource
import com.tencent.devops.ticket.pojo.Cert
import com.tencent.devops.ticket.pojo.CertAndroidWithCredential
import com.tencent.devops.ticket.pojo.CertEnterprise
import com.tencent.devops.ticket.pojo.CertTls
import com.tencent.devops.ticket.pojo.enums.CertType
import com.tencent.devops.ticket.service.CertService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceCertResourceImpl @Autowired constructor(
    private val certService: CertService
) : ServiceCertResource {
    override fun hasUsePermissionAndroidList(
        projectId: String,
        userId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<Cert>> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 20
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = certService.hasPermissionList(
            userId,
            projectId,
            CertType.ANDROID.value,
            BkAuthPermission.USE,
            limit.offset,
            limit.limit
        )
        return Result(Page(pageNotNull, pageSizeNotNull, result.count, result.records))
    }

    override fun getAndroid(projectId: String, certId: String, publicKey: String): Result<CertAndroidWithCredential> {
        checkParams(projectId, certId)
        return Result(certService.queryAndroidByProject(projectId, certId, publicKey))
    }

    override fun getTls(projectId: String, certId: String, publicKey: String): Result<CertTls> {
        checkParams(projectId, certId)
        return Result(certService.queryTlsByProject(projectId, certId, publicKey))
    }

    override fun getEnterprise(projectId: String, certId: String, publicKey: String): Result<CertEnterprise> {
        checkParams(projectId, certId)
        return Result(certService.queryEnterpriseByProject(projectId, certId, publicKey))
    }

    fun checkParams(projectId: String, certId: String) {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (certId.isBlank()) {
            throw ParamBlankException("Invalid certId")
        }
    }
}