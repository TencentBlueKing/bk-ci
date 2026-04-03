package com.tencent.devops.process.service

import com.tencent.devops.auth.api.service.ServiceAuthAuthorizationResource
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.dao.PipelineVisibilityDao
import com.tencent.devops.process.pojo.PipelineVisibility
import com.tencent.devops.process.pojo.PipelineVisibilityType
import com.tencent.devops.project.api.service.ServiceUserResource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineVisibilityService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineVisibilityDao: PipelineVisibilityDao,
    private val client: Client
) {

    fun addVisibility(
        userId: String,
        projectId: String,
        pipelineId: String,
        visibilityList: List<PipelineVisibility>
    ) {
        pipelineVisibilityDao.create(
            dslContext = dslContext,
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            visibilityList = visibilityList
        )
    }

    fun deleteVisibility(
        projectId: String,
        pipelineId: String,
        visibilityList: List<PipelineVisibility>
    ) {
        pipelineVisibilityDao.delete(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            visibilityList = visibilityList
        )
    }

    fun updateVisibility(
        userId: String,
        projectId: String,
        pipelineId: String,
        visibilityList: List<PipelineVisibility>
    ) {
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineVisibilityDao.deleteByPipelineId(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId
            )
            pipelineVisibilityDao.create(
                dslContext = transactionContext,
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                visibilityList = visibilityList
            )
        }
    }

    fun listVisibility(
        projectId: String,
        pipelineId: String,
        page: Int,
        pageSize: Int
    ): SQLPage<PipelineVisibility> {
        val count = pipelineVisibilityDao.count(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        val records = pipelineVisibilityDao.list(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            limit = sqlLimit.limit,
            offset = sqlLimit.offset
        ).map {
            PipelineVisibility(
                type = PipelineVisibilityType.valueOf(it.type),
                scopeId = it.scopeId,
                scopeName = it.scopeName
            )
        }
        return SQLPage(
            count = count.toLong(),
            records = records
        )
    }

    /**
     * 查询请求者有权限的、目标用户(权限代持人)公开的流水线ID集合
     *
     * 流程:
     * 1. 通过 Auth 服务查询 targetUserId 作为权限代持人的流水线ID列表
     * 2. SQL 直接匹配: (TYPE=DEPT AND SCOPE_ID IN 用户组织架构) OR (TYPE=USER AND SCOPE_ID=请求者)
     */
    fun listVisiblePipelineIds(
        requestUserId: String,
        projectId: String,
        targetUserId: String
    ): Set<String> {
        val authPipelineIds = getAuthorizationPipelineIds(
            projectId = projectId,
            targetUserId = targetUserId
        )
        if (authPipelineIds.isEmpty()) {
            return emptySet()
        }

        val userDeptIds = getUserDeptIds(requestUserId)
        return pipelineVisibilityDao.listVisiblePipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            pipelineIds = authPipelineIds,
            requestUserId = requestUserId,
            userDeptIds = userDeptIds
        )
    }

    private fun getAuthorizationPipelineIds(
        projectId: String,
        targetUserId: String
    ): Set<String> {
        return try {
            val resourceType = AuthResourceType.PIPELINE_DEFAULT.value
            client.get(ServiceAuthAuthorizationResource::class).listResourceCodesByHandoverFrom(
                projectId = projectId,
                resourceType = resourceType,
                handoverFrom = targetUserId
            ).data?.toSet() ?: emptySet()
        } catch (e: Exception) {
            logger.warn("Failed to get authorization pipelines for user $targetUserId in $projectId", e)
            emptySet()
        }
    }

    private fun getUserDeptIds(userId: String): Set<String> {
        return try {
            val userInfo = client.get(ServiceUserResource::class).getDetailFromCache(userId).data
            if (userInfo == null) {
                emptySet()
            } else {
                setOfNotNull(
                    userInfo.bgId,
                    userInfo.deptId,
                    userInfo.centerId,
                    userInfo.groupId
                ).filter { it != "0" && it.isNotBlank() }.toSet()
            }
        } catch (e: Exception) {
            logger.warn("Failed to get dept list for user $userId", e)
            emptySet()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineVisibilityService::class.java)
    }
}
