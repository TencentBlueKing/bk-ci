package com.tencent.devops.process.permission.service.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.OwnerUtils
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import javax.ws.rs.core.Response

class V3PipelinePermissionService constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext,
    private val pipelineInfoDao: PipelineInfoDao,
    authProjectApi: AuthProjectApi,
    authResourceApi: AuthResourceApi,
    authPermissionApi: AuthPermissionApi,
    pipelineAuthServiceCode: PipelineAuthServiceCode
) : AbstractPipelinePermissionService(
    authProjectApi = authProjectApi,
    authResourceApi = authResourceApi,
    authPermissionApi = authPermissionApi,
    pipelineAuthServiceCode = pipelineAuthServiceCode
) {
    override fun checkPipelinePermission(userId: String, projectId: String, pipelineId: String, permission: AuthPermission): Boolean {
        if (isProjectOwner(projectId, userId)) {
            return true
        }
        return super.checkPipelinePermission(userId, projectId, pipelineId, permission)
    }

    override fun isProjectUser(userId: String, projectId: String, group: BkAuthGroup?): Boolean {
        if (isProjectOwner(projectId, userId)) {
            return true
        }
        return super.isProjectUser(userId, projectId, group)
    }

    override fun checkPipelinePermission(userId: String, projectId: String, permission: AuthPermission): Boolean {
        logger.info("checkPipelinePermission only check action project[$projectId]")
        if (isProjectOwner(projectId, userId)) {
            logger.info("project owner checkPipelinePermission success |$projectId|$userId")
            return true
        }
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = pipelineAuthServiceCode,
            resourceType = AuthResourceType.PIPELINE_DEFAULT,
            projectCode = projectId,
            resourceCode = projectId,
            permission = AuthPermission.CREATE,
            relationResourceType = AuthResourceType.PROJECT
        )
    }

    override fun validPipelinePermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        permission: AuthPermission,
        message: String?
    ) {
        logger.info("validPipelinePermission V3 impl projectId[$projectId] pipelineId[$pipelineId]")
        if (isProjectOwner(projectId, userId)) {
            logger.info("project owner valid success |$projectId|$userId")
            return
        }

        var authResourceType: AuthResourceType? = null
        if (pipelineId == "*") {
            authResourceType = AuthResourceType.PROJECT
        }
        if (!authPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = pipelineAuthServiceCode,
                resourceType = AuthResourceType.PIPELINE_DEFAULT,
                projectCode = projectId,
                resourceCode = pipelineId,
                permission = permission,
                relationResourceType = authResourceType
            )
        ) {
            val permissionMsg = MessageCodeUtil.getCodeLanMessage(
                messageCode = "${CommonMessageCode.MSG_CODE_PERMISSION_PREFIX}${permission.value}",
                defaultMessage = permission.alias
            )
            throw ErrorCodeException(
                statusCode = Response.Status.FORBIDDEN.statusCode,
                errorCode = ProcessMessageCode.USER_NEED_PIPELINE_X_PERMISSION,
                defaultMessage = message,
                params = arrayOf(permissionMsg)
            )
        }
    }

    override fun supplierForFakePermission(projectId: String): () -> MutableList<String> {
        return {
            val fakeList = mutableListOf<String>()
            pipelineInfoDao.listPipelineIdByProject(dslContext, projectId).forEach {
                fakeList.add(it)
            }
            fakeList
        }
    }

    override fun getResourceByPermission(userId: String, projectId: String, permission: AuthPermission): List<String> {
        val instances = if (isProjectOwner(projectId, userId)) {
            arrayListOf("*")
        } else {
            super.getResourceByPermission(userId, projectId, permission)
        }
        if (instances.contains("*")) {
            logger.info("getResourceByPermission pipelineImpl user[$userId] projectId[$projectId], instances[$instances]")
            val pipelineIds = mutableListOf<String>()
            val pipelineInfos = pipelineInfoDao.listPipelineInfoByProject(dslContext, projectId)
            pipelineInfos?.map {
                pipelineIds.add(it.pipelineId)
            }
            return pipelineIds
        }
        return instances
    }

    private fun isProjectOwner(projectId: String, userId: String): Boolean {
        val cacheOwner = redisOperation.get(OwnerUtils.getOwnerRedisKey(projectId))
        if (cacheOwner.isNullOrEmpty()) {
            val projectVo = client.get(ServiceProjectResource::class).get(projectId).data ?: return false
            val projectCreator = projectVo.creator
            logger.info("pipeline permission get ProjectOwner $projectId | $projectCreator| $userId")
            return if (!projectCreator.isNullOrEmpty()) {
                redisOperation.set(OwnerUtils.getOwnerRedisKey(projectId), projectCreator!!)
                userId == projectCreator
            } else {
                false
            }
        } else {
            return userId == cacheOwner
        }
        return false
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}