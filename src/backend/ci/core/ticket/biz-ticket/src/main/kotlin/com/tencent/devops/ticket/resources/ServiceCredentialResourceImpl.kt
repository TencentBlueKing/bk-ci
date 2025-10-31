/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.ticket.api.ServiceCredentialResource
import com.tencent.devops.ticket.pojo.Credential
import com.tencent.devops.ticket.pojo.CredentialCreate
import com.tencent.devops.ticket.pojo.CredentialInfo
import com.tencent.devops.ticket.pojo.CredentialItemVo
import com.tencent.devops.ticket.pojo.CredentialUpdate
import com.tencent.devops.ticket.pojo.enums.CredentialType
import com.tencent.devops.ticket.pojo.enums.Permission
import com.tencent.devops.ticket.service.CredentialService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("ALL")
class ServiceCredentialResourceImpl @Autowired constructor(
    private val credentialService: CredentialService
) : ServiceCredentialResource {

    @AuditEntry(actionId = ActionId.CREDENTIAL_CREATE)
    @BkTimed(extraTags = ["operate", "create"])
    override fun create(userId: String, projectId: String, credential: CredentialCreate): Result<Boolean> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (credential.credentialId.isBlank()) {
            throw ParamBlankException("Invalid credentialId")
        }
        if (credential.v1.isBlank()) {
            throw ParamBlankException("Invalid credential")
        }
        credentialService.userCreate(userId, projectId, credential, null)
        return Result(true)
    }

    @BkTimed(extraTags = ["operate", "get"])
    override fun get(
        projectId: String,
        credentialId: String,
        publicKey: String,
        padding: Boolean?
    ): Result<CredentialInfo?> {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (credentialId.isBlank()) {
            throw ParamBlankException("Invalid credentialId")
        }
        return Result(
            credentialService.serviceGet(
                projectId = projectId,
                credentialId = credentialId,
                publicKey = publicKey,
                padding = padding ?: false
            )
        )
    }

    override fun getCredentialItem(
        projectId: String,
        credentialId: String,
        publicKey: String,
        padding: Boolean?
    ): Result<CredentialItemVo?> {
        return Result(
            credentialService.getCredentialItem(
                projectId = projectId,
                credentialId = credentialId,
                publicKey = publicKey,
                padding = padding ?: false
            )
        )
    }

    @BkTimed(extraTags = ["operate", "get"])
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

    override fun edit(
        userId: String?,
        projectId: String,
        credentialId: String,
        credential: CredentialUpdate
    ): Result<Boolean> {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (credentialId.isBlank()) {
            throw ParamBlankException("Invalid credentialId")
        }
        if (credential.v1.isBlank()) {
            throw ParamBlankException("Invalid credential")
        }
        credentialService.serviceEdit(
            userId = userId,
            projectId = projectId,
            credentialId = credentialId,
            credential = credential
        )
        return Result(true)
    }

    override fun check(projectId: String, credentialId: String) {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (credentialId.isBlank()) {
            throw ParamBlankException("Invalid credentialId")
        }
        credentialService.serviceCheck(projectId, credentialId)
    }

    @BkTimed(extraTags = ["operate", "get"])
    override fun hasPermissionList(
        userId: String,
        projectId: String,
        credentialTypesString: String?,
        permission: Permission,
        page: Int?,
        pageSize: Int?,
        keyword: String?
    ): Result<Page<Credential>> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        val credentialTypes = credentialTypesString?.split(",")?.map {
            CredentialType.valueOf(it)
        }
        val bkAuthPermission = when (permission) {
            Permission.CREATE -> AuthPermission.CREATE
            Permission.DELETE -> AuthPermission.DELETE
            Permission.LIST -> AuthPermission.LIST
            Permission.VIEW -> AuthPermission.VIEW
            Permission.EDIT -> AuthPermission.EDIT
            Permission.USE -> AuthPermission.USE
        }
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 20
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = credentialService.hasPermissionList(
            userId = userId,
            projectId = projectId,
            credentialTypes = credentialTypes,
            authPermission = bkAuthPermission,
            offset = limit.offset,
            limit = limit.limit,
            keyword = keyword
        )
        return Result(Page(pageNotNull, pageSizeNotNull, result.count, result.records))
    }

    override fun getCredentialByIds(
        projectId: String,
        credentialId: Set<String>
    ): Result<List<Credential>?> {
        return Result(credentialService.getCredentialByIds(projectId, credentialId))
    }
}
