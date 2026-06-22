package com.tencent.devops.environment.service

import com.tencent.devops.auth.api.service.ServiceResourceMemberResource
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.model.environment.tables.records.TEnvRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class EnvCopyService @Autowired constructor(
    private val dslContext: DSLContext,
    private val envDao: EnvDao,
    private val envService: EnvService,
    private val client: Client,
    private val clientTokenService: ClientTokenService
) {

    fun copyAcrossProject(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String,
        envHashId: String?
    ) {
        if (!envHashId.isNullOrBlank()) {
            try {
                copySingleEnvironment(
                    userId = userId,
                    sourceProjectId = sourceProjectId,
                    targetProjectId = targetProjectId,
                    sourceEnv = envDao.get(
                        dslContext = dslContext,
                        projectId = sourceProjectId,
                        envId = HashUtil.decodeIdToLong(envHashId)
                    )
                )
            } catch (ignored: Exception) {
                logger.warn("get source environment failed|$sourceProjectId|$envHashId", ignored)
            }
            return
        }
        copyAllEnvironmentsByPage(userId, sourceProjectId, targetProjectId)
    }

    private fun copyAllEnvironmentsByPage(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String
    ) {
        val pageSize = 100
        var offset = 0
        val total = envDao.countByProject(dslContext, sourceProjectId)
        while (offset < total) {
            envDao.listPage(dslContext, offset, pageSize, sourceProjectId).forEach { sourceEnv ->
                copySingleEnvironment(
                    userId = userId,
                    sourceProjectId = sourceProjectId,
                    targetProjectId = targetProjectId,
                    sourceEnv = sourceEnv
                )
            }
            offset += pageSize
        }
    }

    private fun copySingleEnvironment(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String,
        sourceEnv: TEnvRecord
    ) {
        val sourceEnvHashId = HashUtil.encodeLongId(sourceEnv.envId)
        try {
            if (envDao.getByEnvName(dslContext, targetProjectId, sourceEnv.envName) != null) {
                logger.warn(
                    "environment already exists, skip copy|$sourceProjectId|$targetProjectId|${sourceEnv.envName}"
                )
                return
            }
            val targetEnvId = envService.createEnvAndRelateSameNameNodes(
                userId = userId,
                sourceProjectId = sourceProjectId,
                targetProjectId = targetProjectId,
                sourceEnvHashId = sourceEnvHashId
            )
            copyEnvGroupMembersSafely(
                sourceProjectId = sourceProjectId,
                targetProjectId = targetProjectId,
                sourceEnvHashId = sourceEnvHashId,
                targetEnvHashId = targetEnvId.hashId
            )
        } catch (ignored: Exception) {
            logger.warn(
                "copy environment failed|$sourceProjectId|$targetProjectId|${sourceEnv.envName}",
                ignored
            )
        }
    }

    private fun copyEnvGroupMembersSafely(
        sourceProjectId: String,
        targetProjectId: String,
        sourceEnvHashId: String,
        targetEnvHashId: String
    ) {
        try {
            client.get(ServiceResourceMemberResource::class).copyResourceGroupMembers(
                token = clientTokenService.getSystemToken() ?: "",
                sourceProjectCode = sourceProjectId,
                resourceType = AuthResourceType.ENVIRONMENT_ENVIRONMENT.value,
                sourceResourceCode = sourceEnvHashId,
                targetProjectCode = targetProjectId,
                targetResourceCode = targetEnvHashId
            )
        } catch (ignored: Exception) {
            logger.warn(
                "copy environment group members failed|$sourceProjectId|$targetProjectId|" +
                    "$sourceEnvHashId|$targetEnvHashId",
                ignored
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EnvCopyService::class.java)
    }
}
