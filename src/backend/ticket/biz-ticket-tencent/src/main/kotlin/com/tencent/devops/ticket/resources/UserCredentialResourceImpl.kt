package com.tencent.devops.ticket.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.ticket.api.UserCredentialResource
import com.tencent.devops.ticket.pojo.Credential
import com.tencent.devops.ticket.pojo.CredentialCreate
import com.tencent.devops.ticket.pojo.CredentialUpdate
import com.tencent.devops.ticket.pojo.CredentialWithPermission
import com.tencent.devops.ticket.pojo.enums.CredentialType
import com.tencent.devops.ticket.pojo.enums.Permission
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserCredentialResourceImpl @Autowired constructor(
    private val credentialService: CredentialServiceImpl
) : UserCredentialResource {
    override fun hasCreatePermission(
        userId: String,
        projectId: String
    ): Result<Boolean> {
        return Result(credentialService.hasCreatePermission(userId, projectId))
    }

    override fun create(
        userId: String,
        projectId: String,
        credential: CredentialCreate
    ): Result<Boolean> {
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
        credentialService.userCreate(userId, projectId, credential)
        return Result(true)
    }

    override fun delete(
        userId: String,
        projectId: String,
        credentialId: String
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
        credentialService.userDelete(userId, projectId, credentialId)
        return Result(true)
    }

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
        val result = credentialService.userList(userId, projectId, credentialTypes, limit.offset, limit.limit, keyword)
        return Result(Page(pageNotNull, pageSizeNotNull, result.count, result.records))
    }

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
        val result = credentialService.hasPermissionList(userId, projectId, credentialTypes, bkAuthPermission, limit.offset, limit.limit, keyword)
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
        val bkAuthPermission = when (permission) {
            Permission.CREATE -> AuthPermission.CREATE
            Permission.DELETE -> AuthPermission.DELETE
            Permission.LIST -> AuthPermission.LIST
            Permission.VIEW -> AuthPermission.VIEW
            Permission.EDIT -> AuthPermission.EDIT
            Permission.USE -> AuthPermission.USE
        }

        val result = credentialService.hasPermissionList(userId, projectId, credentialTypes, bkAuthPermission, null,
            null, keyword)
        return Result(result.records)
    }

    override fun show(
        userId: String,
        projectId: String,
        credentialId: String
    ): Result<CredentialWithPermission> {
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

    override fun edit(userId: String, projectId: String, credentialId: String, credential: CredentialUpdate): Result<Boolean> {
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
}