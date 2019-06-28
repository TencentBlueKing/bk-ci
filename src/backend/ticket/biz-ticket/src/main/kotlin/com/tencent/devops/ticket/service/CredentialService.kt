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

package com.tencent.devops.ticket.service

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.auth.api.BkAuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.api.ServiceBuildResource
import com.tencent.devops.ticket.dao.CredentialDao
import com.tencent.devops.ticket.pojo.Credential
import com.tencent.devops.ticket.pojo.CredentialCreate
import com.tencent.devops.ticket.pojo.CredentialInfo
import com.tencent.devops.ticket.pojo.CredentialPermissions
import com.tencent.devops.ticket.pojo.CredentialUpdate
import com.tencent.devops.ticket.pojo.CredentialWithPermission
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Base64
import javax.ws.rs.NotFoundException

@Service
class CredentialService @Autowired constructor(
    private val credentialHelper: CredentialHelper,
    private val credentialPermissionService: CredentialPermissionService,
    private val dslContext: DSLContext,
    private val client: Client,
    private val credentialDao: CredentialDao
) {
    private val credentialIdMaxSize = 32

    fun userCreate(userId: String, projectId: String, credential: CredentialCreate) {
        credentialPermissionService.validatePermission(
            userId,
            projectId,
            BkAuthPermission.CREATE,
            "用户($userId)在工程($projectId)下没有凭据创建权限"
        )

        if (credentialDao.has(dslContext, projectId, credential.credentialId)) {
            throw OperationException("名称${credential.credentialId}已存在")
        }
        if (!credentialHelper.isValid(credential)) {
            throw OperationException("凭证格式不正确")
        }
        if (credential.credentialId.length > credentialIdMaxSize) {
            throw OperationException("凭证ID不能超过32位")
        }

        credentialPermissionService.createResource(userId, projectId, credential.credentialId)
        credentialDao.create(
            dslContext = dslContext,
            projectId = projectId,
            credentialUserId = userId,
            credentialId = credential.credentialId,
            credentialType = credential.credentialType.name,
            credentialV1 = credentialHelper.encryptCredential(credential.v1)!!,
            credentialV2 = credentialHelper.encryptCredential(credential.v2),
            credentialV3 = credentialHelper.encryptCredential(credential.v3),
            credentialV4 = credentialHelper.encryptCredential(credential.v4),
            credentialRemark = credential.credentialRemark
        )
    }

    fun userEdit(userId: String, projectId: String, credentialId: String, credential: CredentialUpdate) {
        credentialPermissionService.validatePermission(
            userId,
            projectId,
            credentialId,
            BkAuthPermission.EDIT,
            "用户($userId)在工程($projectId)下没有凭据($credentialId)的编辑权限"
        )

        if (!credentialDao.has(dslContext, projectId, credentialId)) {
            throw OperationException("凭证$credentialId 不存在")
        }
        if (!credentialHelper.isValid(credential)) {
            throw OperationException("凭证格式不正确")
        }

        credentialDao.updateIgnoreNull(
            dslContext = dslContext,
            projectId = projectId,
            credentialId = credentialId,
            credentialV1 = credentialHelper.encryptCredential(credential.v1),
            credentialV2 = credentialHelper.encryptCredential(credential.v2),
            credentialV3 = credentialHelper.encryptCredential(credential.v3),
            credentialV4 = credentialHelper.encryptCredential(credential.v4),
            credentialRemark = credential.credentialRemark
        )
    }

    fun userDelete(userId: String, projectId: String, credentialId: String) {
        credentialPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            resourceCode = credentialId,
            bkAuthPermission = BkAuthPermission.DELETE,
            message = "用户($userId)在工程($projectId)下没有凭据($credentialId)的删除权限"
        )

        credentialPermissionService.deleteResource(projectId, credentialId)
        credentialDao.delete(dslContext, projectId, credentialId)
    }

    fun userList(
        userId: String,
        projectId: String,
        credentialTypes: List<CredentialType>?,
        offset: Int,
        limit: Int
    ): SQLPage<CredentialWithPermission> {
        val permissionToListMap = credentialPermissionService.filterCredentials(
            userId = userId,
            projectId = projectId,
            bkAuthPermissions = setOf(
                BkAuthPermission.LIST,
                BkAuthPermission.DELETE,
                BkAuthPermission.VIEW,
                BkAuthPermission.EDIT
            )
        )
        val hasListPermissionCredentialIdList = permissionToListMap[BkAuthPermission.LIST]!!
        val hasDeletePermissionCredentialIdList = permissionToListMap[BkAuthPermission.DELETE]!!
        val hasViewPermissionCredentialIdList = permissionToListMap[BkAuthPermission.VIEW]!!
        val hasEditPermissionCredentialIdList = permissionToListMap[BkAuthPermission.EDIT]!!

        val count = credentialDao.countByProject(
            dslContext,
            projectId,
            credentialTypes?.toSet(),
            hasListPermissionCredentialIdList.toSet()
        )
        val credentialRecordList = credentialDao.listByProject(
            dslContext,
            projectId,
            credentialTypes?.toSet(),
            hasListPermissionCredentialIdList.toSet(),
            offset,
            limit
        )
        val credentialList = credentialRecordList.map {
            val hasDeletePermission = hasDeletePermissionCredentialIdList.contains(it.credentialId)
            val hasViewPermission = hasViewPermissionCredentialIdList.contains(it.credentialId)
            val hasEditPermission = hasEditPermissionCredentialIdList.contains(it.credentialId)
            CredentialWithPermission(
                credentialId = it.credentialId,
                credentialType = CredentialType.valueOf(it.credentialType),
                credentialRemark = it.credentialRemark,
                updatedTime = it.updatedTime.timestamp(),
                v1 = credentialHelper.decryptCredential(it.credentialV1)!!,
                v2 = credentialHelper.decryptCredential(it.credentialV2),
                v3 = credentialHelper.decryptCredential(it.credentialV3),
                v4 = credentialHelper.decryptCredential(it.credentialV4),
                permissions = CredentialPermissions(
                    hasDeletePermission,
                    hasViewPermission,
                    hasEditPermission
                )
            )
        }
        return SQLPage(count, credentialList)
    }

    fun hasPermissionList(
        userId: String,
        projectId: String,
        credentialTypes: List<CredentialType>?,
        bkAuthPermission: BkAuthPermission,
        offset: Int,
        limit: Int
    ): SQLPage<Credential> {
        val hasPermissionList = credentialPermissionService.filterCredential(userId, projectId, bkAuthPermission)

        val count =
            credentialDao.countByProject(dslContext, projectId, credentialTypes?.toSet(), hasPermissionList.toSet())
        val credentialRecordList = credentialDao.listByProject(
            dslContext,
            projectId,
            credentialTypes?.toSet(),
            hasPermissionList.toSet(),
            offset,
            limit
        )
        val credentialList = credentialRecordList.map {
            Credential(
                credentialId = it.credentialId,
                credentialType = CredentialType.valueOf(it.credentialType),
                credentialRemark = it.credentialRemark,
                updatedTime = it.createdTime.timestamp(),
                v1 = credentialHelper.credentialMixer,
                v2 = credentialHelper.credentialMixer,
                v3 = credentialHelper.credentialMixer,
                v4 = credentialHelper.credentialMixer
            )
        }
        return SQLPage(count, credentialList)
    }

    fun serviceList(projectId: String, offset: Int, limit: Int): SQLPage<Credential> {
        val count = credentialDao.countByProject(dslContext, projectId)
        val credentialRecords = credentialDao.listByProject(dslContext, projectId, offset, limit)
        val result = credentialRecords.map {
            Credential(
                credentialId = it.credentialId,
                credentialType = CredentialType.valueOf(it.credentialType),
                credentialRemark = it.credentialRemark,
                updatedTime = it.createdTime.timestamp(),
                v1 = credentialHelper.credentialMixer,
                v2 = credentialHelper.credentialMixer,
                v3 = credentialHelper.credentialMixer,
                v4 = credentialHelper.credentialMixer
            )
        }
        return SQLPage(count, result)
    }

    fun serviceCheck(projectId: String, credentialId: String) {
        if (!credentialDao.has(dslContext, projectId, credentialId)) {
            throw NotFoundException("Credential $credentialId does not exists")
        }
    }

    fun userShow(userId: String, projectId: String, credentialId: String): CredentialWithPermission {
        credentialPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            resourceCode = credentialId,
            bkAuthPermission = BkAuthPermission.VIEW,
            message = "用户($userId)在工程($projectId)下没有凭据($credentialId)的查看权限"
        )

        val hasViewPermission = true
        val hasDeletePermission =
            credentialPermissionService.validatePermission(userId, projectId, credentialId, BkAuthPermission.DELETE)
        val hasEditPermission =
            credentialPermissionService.validatePermission(userId, projectId, credentialId, BkAuthPermission.EDIT)

        val credentialRecord = credentialDao.get(dslContext, projectId, credentialId)

        return CredentialWithPermission(
            credentialId = credentialId,
            credentialType = CredentialType.valueOf(credentialRecord.credentialType),
            credentialRemark = credentialRecord.credentialRemark,
            updatedTime = credentialRecord.updatedTime.timestamp(),
            v1 = credentialHelper.decryptCredential(credentialRecord.credentialV1)!!,
            v2 = credentialHelper.decryptCredential(credentialRecord.credentialV2),
            v3 = credentialHelper.decryptCredential(credentialRecord.credentialV3),
            v4 = credentialHelper.decryptCredential(credentialRecord.credentialV4),
            permissions = CredentialPermissions(
                hasDeletePermission,
                hasViewPermission,
                hasEditPermission
            )
        )
    }

    fun userGet(userId: String, projectId: String, credentialId: String): CredentialWithPermission {
        credentialPermissionService.validatePermission(
            userId,
            projectId,
            credentialId,
            BkAuthPermission.VIEW,
            "用户($userId)在工程($projectId)下没有凭据($credentialId)的查看权限"
        )

        val hasViewPermission = true
        val hasDeletePermission =
            credentialPermissionService.validatePermission(userId, projectId, credentialId, BkAuthPermission.DELETE)
        val hasEditPermission =
            credentialPermissionService.validatePermission(userId, projectId, credentialId, BkAuthPermission.EDIT)

        val credentialRecord = credentialDao.get(dslContext, projectId, credentialId)
        return CredentialWithPermission(
            credentialId = credentialId,
            credentialType = CredentialType.valueOf(credentialRecord.credentialType),
            credentialRemark = credentialRecord.credentialRemark,
            updatedTime = credentialRecord.updatedTime.timestamp(),
            v1 = credentialHelper.credentialMixer,
            v2 = credentialHelper.credentialMixer,
            v3 = credentialHelper.credentialMixer,
            v4 = credentialHelper.credentialMixer,
            permissions = CredentialPermissions(
                hasDeletePermission,
                hasViewPermission,
                hasEditPermission
            )
        )
    }

    fun buildGet(buildId: String, credentialId: String, publicKey: String): CredentialInfo {
        val buildBasicInfoResult = client.get(ServiceBuildResource::class).serviceBasic(buildId)
        if (buildBasicInfoResult.isNotOk()) {
            throw RemoteServiceException("Failed to build the basic information based on the buildId")
        }
        val buildBasicInfo = buildBasicInfoResult.data
            ?: throw RemoteServiceException("Failed to build the basic information based on the buildId")
        return serviceGet(buildBasicInfo.projectId, credentialId, publicKey)
    }

    fun serviceGet(projectId: String, credentialId: String, publicKey: String): CredentialInfo {
        val credentialRecord = credentialDao.get(dslContext, projectId, credentialId)

        val publicKeyByteArray = Base64.getDecoder().decode(publicKey)
        val serverDHKeyPair = DHUtil.initKey(publicKeyByteArray)
        val serverPublicKeyByteArray = serverDHKeyPair.publicKey
        val serverPrivateKeyByteArray = serverDHKeyPair.privateKey
        val serverBase64PublicKey = String(Base64.getEncoder().encode(serverPublicKeyByteArray))

        val credentialV1 = credentialHelper.encryptCredential(
            aesEncryptedCredential = credentialRecord.credentialV1,
            publicKeyByteArray = publicKeyByteArray,
            serverPrivateKeyByteArray = serverPrivateKeyByteArray
        )!!
        val credentialV2 = credentialHelper.encryptCredential(
            aesEncryptedCredential = credentialRecord.credentialV2,
            publicKeyByteArray = publicKeyByteArray,
            serverPrivateKeyByteArray = serverPrivateKeyByteArray
        )
        val credentialV3 = credentialHelper.encryptCredential(
            aesEncryptedCredential = credentialRecord.credentialV3,
            publicKeyByteArray = publicKeyByteArray,
            serverPrivateKeyByteArray = serverPrivateKeyByteArray
        )
        val credentialV4 = credentialHelper.encryptCredential(
            aesEncryptedCredential = credentialRecord.credentialV4,
            publicKeyByteArray = publicKeyByteArray,
            serverPrivateKeyByteArray = serverPrivateKeyByteArray
        )

        return CredentialInfo(
            publicKey = serverBase64PublicKey,
            credentialType = CredentialType.valueOf(credentialRecord.credentialType),
            v1 = credentialV1,
            v2 = credentialV2,
            v3 = credentialV3,
            v4 = credentialV4
        )
    }

    fun serviceGet(projectId: String, credentialId: String): Credential {
        val record = credentialDao.get(dslContext, projectId, credentialId)

        return Credential(
            credentialId = record.credentialId,
            credentialType = CredentialType.valueOf(record.credentialType),
            credentialRemark = record.credentialRemark,
            updatedTime = record.updatedTime.timestamp(),
            v1 = credentialHelper.decryptCredential(record.credentialV1)!!,
            v2 = credentialHelper.decryptCredential(record.credentialV2),
            v3 = credentialHelper.decryptCredential(record.credentialV3),
            v4 = credentialHelper.decryptCredential(record.credentialV4)
        )
    }
}
