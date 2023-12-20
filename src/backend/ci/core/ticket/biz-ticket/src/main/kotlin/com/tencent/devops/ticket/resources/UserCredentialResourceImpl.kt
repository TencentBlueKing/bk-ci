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

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.ticket.api.UserCredentialResource
import com.tencent.devops.ticket.pojo.Credential
import com.tencent.devops.ticket.pojo.CredentialCreate
import com.tencent.devops.ticket.pojo.CredentialSettingUpdate
import com.tencent.devops.ticket.pojo.CredentialUpdate
import com.tencent.devops.ticket.pojo.CredentialWithPermission
import com.tencent.devops.ticket.pojo.enums.CredentialType
import com.tencent.devops.ticket.pojo.enums.Permission
import com.tencent.devops.ticket.service.CredentialPermissionService
import com.tencent.devops.ticket.service.CredentialService
import org.springframework.beans.factory.annotation.Autowired

@Suppress("ALL")
@RestResource
class UserCredentialResourceImpl @Autowired constructor(
    private val credentialService: CredentialService,
    private val credentialPermissionService: CredentialPermissionService
) : UserCredentialResource {
    override fun hasCreatePermission(userId: String, projectId: String): Result<Boolean> {
        return Result(credentialPermissionService.validatePermission(userId, projectId, AuthPermission.CREATE))
    }

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
        credentialService.userCreate(
            userId = userId,
            projectId = projectId,
            credential = credential,
            authGroupList = null
        )
        return Result(true)
    }

    @AuditEntry(actionId = ActionId.CREDENTIAL_DELETE)
    override fun delete(userId: String, projectId: String, credentialId: String): Result<Boolean> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (credentialId.isBlank()) {
            throw ParamBlankException("Invalid credentialId")
        }
        credentialService.userDelete(userId, projectId, credentialId)
        return Result(true)
    }

    @BkTimed(extraTags = ["operate", "get"])
    override fun list(
        userId: String,
        projectId: String,
        credentialTypesString: String?,
        page: Int?,
        pageSize: Int?,
        keyword: String?
    ): Result<Page<CredentialWithPermission>> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        val credentialTypes = credentialTypesString?.split(",")?.map {
            CredentialType.valueOf(it)
        }
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 20
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = credentialService.userList(
            userId = userId,
            projectId = projectId,
            credentialTypes = credentialTypes,
            offset = limit.offset,
            limit = limit.limit,
            keyword = keyword
        )
        return Result(Page(pageNotNull, pageSizeNotNull, result.count, result.records))
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
        val authPermission = when (permission) {
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
            authPermission = authPermission,
            offset = limit.offset,
            limit = limit.limit,
            keyword = keyword
        )
        return Result(Page(pageNotNull, pageSizeNotNull, result.count, result.records))
    }

    override fun getHasPermissionList(
        userId: String,
        projectId: String,
        credentialTypesString: String?,
        permission: Permission,
        keyword: String?
    ): Result<List<Credential>> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        val credentialTypes = credentialTypesString?.split(",")?.map {
            CredentialType.valueOf(it)
        }
        val authPermission = when (permission) {
            Permission.CREATE -> AuthPermission.CREATE
            Permission.DELETE -> AuthPermission.DELETE
            Permission.LIST -> AuthPermission.LIST
            Permission.VIEW -> AuthPermission.VIEW
            Permission.EDIT -> AuthPermission.EDIT
            Permission.USE -> AuthPermission.USE
        }

        val result = credentialService.hasPermissionList(
            userId = userId,
            projectId = projectId,
            credentialTypes = credentialTypes,
            authPermission = authPermission,
            offset = null,
            limit = null,
            keyword = keyword
        )
        return Result(result.records)
    }

    @AuditEntry(actionId = ActionId.CREDENTIAL_VIEW)
    @BkTimed(extraTags = ["operate", "get"])
    override fun show(userId: String, projectId: String, credentialId: String): Result<CredentialWithPermission> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (credentialId.isBlank()) {
            throw ParamBlankException("Invalid credentialId")
        }
        return Result(credentialService.userShow(userId, projectId, credentialId))
    }

    @AuditEntry(actionId = ActionId.CREDENTIAL_VIEW)
    @BkTimed(extraTags = ["operate", "get"])
    override fun get(userId: String, projectId: String, credentialId: String): Result<CredentialWithPermission> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (credentialId.isBlank()) {
            throw ParamBlankException("Invalid credentialId")
        }
        return Result(credentialService.userGet(userId, projectId, credentialId))
    }

    @AuditEntry(actionId = ActionId.CREDENTIAL_EDIT)
    override fun edit(
        userId: String,
        projectId: String,
        credentialId: String,
        credential: CredentialUpdate
    ): Result<Boolean> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (credentialId.isBlank()) {
            throw ParamBlankException("Invalid credentialId")
        }
        if (credential.v1.isBlank()) {
            throw ParamBlankException("Invalid credential")
        }
        credentialService.userEdit(userId, projectId, credentialId, credential)
        return Result(true)
    }

    @AuditEntry(actionId = ActionId.CREDENTIAL_EDIT)
    override fun editSetting(
        userId: String,
        projectId: String,
        credentialId: String,
        credentialSetting: CredentialSettingUpdate
    ): Result<Boolean> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (credentialId.isBlank()) {
            throw ParamBlankException("Invalid credentialId")
        }
        return Result(
            credentialService.userSettingEdit(userId, projectId, credentialId, credentialSetting)
        )
    }
}
