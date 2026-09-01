package com.tencent.devops.ticket.service

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.auth.api.service.ServiceResourceMemberResource
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.model.ticket.tables.records.TCredentialRecord
import com.tencent.devops.ticket.dao.CredentialDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CredentialCopyService @Autowired constructor(
    private val dslContext: DSLContext,
    private val credentialDao: CredentialDao,
    private val client: Client,
    private val clientTokenService: ClientTokenService
) {

    fun copyAcrossProject(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String,
        credentialId: String?
    ) {
        if (!credentialId.isNullOrBlank()) {
            try {
                copySingleCredential(
                    userId = userId,
                    sourceProjectId = sourceProjectId,
                    targetProjectId = targetProjectId,
                    sourceRecord = credentialDao.get(dslContext, sourceProjectId, credentialId)
                )
            } catch (ignored: Exception) {
                logger.warn("get source credential failed|$sourceProjectId|$credentialId", ignored)
            }
            return
        }
        copyAllCredentialsByPage(userId, sourceProjectId, targetProjectId)
    }

    private fun copyAllCredentialsByPage(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String
    ) {
        val pageSize = 100
        var offset = 0
        val total = credentialDao.countByProject(dslContext, sourceProjectId)
        while (offset < total) {
            credentialDao.listByProject(dslContext, sourceProjectId, offset, pageSize).forEach { sourceRecord ->
                copySingleCredential(
                    userId = userId,
                    sourceProjectId = sourceProjectId,
                    targetProjectId = targetProjectId,
                    sourceRecord = sourceRecord
                )
            }
            offset += pageSize
        }
    }

    private fun copySingleCredential(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String,
        sourceRecord: TCredentialRecord
    ) {
        val copiedCredentialId = sourceRecord.credentialId
        try {
            if (credentialDao.has(dslContext, targetProjectId, copiedCredentialId)) {
                logger.warn(
                    "credential already exists, skip copy|$sourceProjectId|$targetProjectId|$copiedCredentialId"
                )
                return
            }
            credentialDao.create(
                dslContext = dslContext,
                projectId = targetProjectId,
                credentialUserId = sourceRecord.credentialUserId,
                credentialId = copiedCredentialId,
                credentialName = sourceRecord.credentialName ?: copiedCredentialId,
                credentialType = sourceRecord.credentialType,
                credentialV1 = sourceRecord.credentialV1,
                credentialV2 = sourceRecord.credentialV2,
                credentialV3 = sourceRecord.credentialV3,
                credentialV4 = sourceRecord.credentialV4,
                credentialRemark = sourceRecord.credentialRemark
            )
            createCredentialResourceSafely(
                userId = userId,
                targetProjectId = targetProjectId,
                credentialId = copiedCredentialId
            )
            copyCredentialGroupMembersSafely(
                sourceProjectId = sourceProjectId,
                targetProjectId = targetProjectId,
                credentialId = copiedCredentialId
            )
        } catch (ignored: Exception) {
            logger.warn(
                "copy credential failed|$sourceProjectId|$targetProjectId|$copiedCredentialId",
                ignored
            )
        }
    }

    private fun createCredentialResourceSafely(
        userId: String,
        targetProjectId: String,
        credentialId: String
    ) {
        try {
            client.get(ServicePermissionAuthResource::class).resourceCreateRelation(
                userId = userId,
                token = clientTokenService.getSystemToken() ?: "",
                projectCode = targetProjectId,
                resourceType = AuthResourceType.TICKET_CREDENTIAL.value,
                resourceCode = credentialId,
                resourceName = credentialId
            )
        } catch (ignored: Exception) {
            logger.warn(
                "create credential resource failed|$targetProjectId|$credentialId",
                ignored
            )
        }
    }

    private fun copyCredentialGroupMembersSafely(
        sourceProjectId: String,
        targetProjectId: String,
        credentialId: String
    ) {
        try {
            client.get(ServiceResourceMemberResource::class).copyResourceGroupMembers(
                token = clientTokenService.getSystemToken() ?: "",
                sourceProjectCode = sourceProjectId,
                resourceType = AuthResourceType.TICKET_CREDENTIAL.value,
                sourceResourceCode = credentialId,
                targetProjectCode = targetProjectId,
                targetResourceCode = credentialId
            )
        } catch (ignored: Exception) {
            logger.warn(
                "copy credential group members failed|$sourceProjectId|$targetProjectId|$credentialId",
                ignored
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CredentialCopyService::class.java)
    }
}
