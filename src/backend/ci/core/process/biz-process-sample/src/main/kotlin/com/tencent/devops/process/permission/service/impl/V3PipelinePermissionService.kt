package com.tencent.devops.process.permission.service.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import javax.ws.rs.core.Response

class V3PipelinePermissionService constructor(
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

    override fun checkPipelinePermission(userId: String, projectId: String, permission: AuthPermission): Boolean {
        logger.info("checkPipelinePermission only check action project[$projectId]")
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

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}