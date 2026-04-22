package com.tencent.devops.process.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.auth.api.service.ServiceAuthAuthorizationResource
import com.tencent.devops.auth.api.service.ServiceDeptResource
import com.tencent.devops.auth.pojo.vo.UserAndDeptInfoVo
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.util.CacheHelper
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineVisibilityDao
import com.tencent.devops.process.pojo.PipelineVisibility
import com.tencent.devops.process.pojo.PipelineVisibilityType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineVisibilityService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineVisibilityDao: PipelineVisibilityDao,
    private val client: Client
) {

    private val userDeptIdCache = CacheHelper.createCache<String, Set<String>>(
        duration = 5,
        maxSize = 5000
    )

    private val userInfoCache = CacheHelper.createCache<String, UserAndDeptInfoVo?>(
        duration = 5,
        maxSize = 5000
    )

    /**
     * 创作流创建时，初始化可见性
     */
    fun initVisibility(
        transactionContext: DSLContext? = null,
        userId: String,
        projectId: String,
        pipelineId: String
    ) {
        val userInfo = userInfoCache.get(userId) {
            client.get(ServiceDeptResource::class).getUserInfo(userId = userId, name = userId).data
        } ?: return
        val userDepartments = userInfo.deptInfo?.lastOrNull {
            !it.fullName.isNullOrBlank()
        }?.let {
            listOf(it.fullName!!)
        } ?: emptyList()
        pipelineVisibilityDao.create(
            dslContext = transactionContext ?: dslContext,
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            authUser = userId,
            visibilityList = listOf(
                PipelineVisibility(
                    type = PipelineVisibilityType.USER,
                    scopeId = userId,
                    scopeName = userInfo.name,
                    fullName = userId,
                    userDepartments = userDepartments
                )
            )
        )
    }

    fun addVisibility(
        userId: String,
        projectId: String,
        pipelineId: String,
        visibilityList: List<PipelineVisibility>
    ) {
        val authUser = getAuthUser(projectId, pipelineId)
        logger.info("add pipeline visibility|$userId|$projectId|$pipelineId|$authUser")
        pipelineVisibilityDao.create(
            dslContext = dslContext,
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            authUser = authUser,
            visibilityList = visibilityList
        )
    }

    fun deleteVisibility(
        projectId: String,
        pipelineId: String,
        scopeIds: List<String>
    ) {
        pipelineVisibilityDao.deleteByScopeIds(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            scopeIds = scopeIds
        )
    }

    fun deleteByPipelineId(
        transactionContext: DSLContext? = null,
        projectId: String,
        pipelineId: String
    ) {
        pipelineVisibilityDao.deleteByPipelineId(
            dslContext = transactionContext ?: dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }

    fun listVisibility(
        projectId: String,
        pipelineId: String,
        page: Int,
        pageSize: Int,
        keyword: String? = null
    ): SQLPage<PipelineVisibility> {
        val count = pipelineVisibilityDao.count(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            keyword = keyword
        )
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        val records = pipelineVisibilityDao.list(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            keyword = keyword,
            limit = sqlLimit.limit,
            offset = sqlLimit.offset
        ).map {
            PipelineVisibility(
                type = PipelineVisibilityType.valueOf(it.type),
                scopeId = it.scopeId,
                scopeName = it.scopeName,
                fullName = it.fullName,
                userDepartments =
                    it.userDepartments?.let { u ->
                        JsonUtil.to(u, object : TypeReference<List<String>>() {})
                    }
            )
        }
        return SQLPage(
            count = count.toLong(),
            records = records
        )
    }

    fun updateAuthUser(
        projectId: String,
        pipelineId: String,
        authUser: String
    ) {
        pipelineVisibilityDao.updateAuthUser(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            authUser = authUser
        )
    }

    /**
     * 检查用户是否在流水线可见范围内（无记录视为无限制）
     */
    fun hasVisibility(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Boolean {
        val userDeptIds = getUserDeptIds(userId)
        return pipelineVisibilityDao.countVisibilityPipeline(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            userDeptIds = userDeptIds
        ) > 0
    }

    /**
     * 查询目标用户(权限代持人)公开的、请求者可见的流水线ID列表（支持分页和候选ID过滤）
     */
    fun listVisiblePipelineIds(
        requestUserId: String,
        projectId: String,
        targetUserId: String,
        pipelineIds: Set<String>? = null,
        limit: Int? = null,
        offset: Int? = null
    ): List<String> {
        val userDeptIds = getUserDeptIds(requestUserId)
        return pipelineVisibilityDao.listVisiblePipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            authUser = targetUserId,
            requestUserId = requestUserId,
            userDeptIds = userDeptIds,
            pipelineIds = pipelineIds,
            limit = limit,
            offset = offset
        )
    }

    private fun getAuthUser(projectId: String, pipelineId: String): String {
        return client.get(ServiceAuthAuthorizationResource::class).getResourceAuthorization(
            projectId = projectId,
            resourceType = AuthResourceType.getAuthResourceTypeByChannel(AuthResourceType.PIPELINE_DEFAULT).value,
            resourceCode = pipelineId
        ).data?.handoverFrom ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_AUTH_USER_NOT_EXISTS,
            params = arrayOf(pipelineId)
        )
    }

    private fun getUserDeptIds(userId: String): Set<String> {
        return userDeptIdCache.get(userId) {
            try {
                client.get(ServiceDeptResource::class).getUserDeptIds(userId).data ?: emptySet()
            } catch (e: Exception) {
                logger.warn("Failed to get dept list for user $userId", e)
                emptySet()
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineVisibilityService::class.java)
    }
}
