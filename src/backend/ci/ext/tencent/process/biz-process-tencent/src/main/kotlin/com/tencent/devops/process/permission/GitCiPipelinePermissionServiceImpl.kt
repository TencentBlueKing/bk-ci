//package com.tencent.devops.process.permission
//
//import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
//import com.tencent.devops.common.api.exception.PermissionForbiddenException
//import com.tencent.devops.common.auth.api.AuthPermission
//import com.tencent.devops.common.auth.api.AuthResourceType
//import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
//import com.tencent.devops.common.client.Client
//import com.tencent.devops.common.client.ClientTokenService
//import com.tencent.devops.process.engine.dao.PipelineInfoDao
//import org.jooq.DSLContext
//import org.springframework.beans.factory.annotation.Autowired
//
//class GitCiPipelinePermissionServiceImpl @Autowired constructor(
//    val client: Client,
//    val pipelineInfoDao: PipelineInfoDao,
//    val dslContext: DSLContext,
//    val checkTokenService: ClientTokenService
//) : PipelinePermissionService {
//
//    override fun checkPipelinePermission(
//        userId: String,
//        projectId: String,
//        permission: AuthPermission
//    ): Boolean {
//        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
//            userId = userId,
//            token = checkTokenService.getSystemToken(null) ?: "",
//            action = permission.value,
//            projectCode = projectId,
//            resourceCode = AuthResourceType.PIPELINE_DEFAULT.value
//        ).data ?: false
//    }
//
//    override fun checkPipelinePermission(
//        userId: String,
//        projectId: String,
//        pipelineId: String,
//        permission: AuthPermission
//    ): Boolean {
//        return checkPipelinePermission(userId, projectId, permission)
//    }
//
//    override fun validPipelinePermission(
//        userId: String,
//        projectId: String,
//        pipelineId: String,
//        permission: AuthPermission,
//        message: String?
//    ) {
//        val checkPermission = checkPipelinePermission(userId, projectId, permission)
//        if (!checkPermission) {
//            throw PermissionForbiddenException(message)
//        }
//    }
//
//    override fun getResourceByPermission(
//        userId: String,
//        projectId: String,
//        permission: AuthPermission
//    ): List<String> {
//        if (!checkPipelinePermission(userId, projectId, permission)) {
//            return emptyList()
//        }
//        return getProjectAllInstance(projectId)
//    }
//
//    override fun createResource(userId: String, projectId: String, pipelineId: String, pipelineName: String) {
//        return
//    }
//
//    override fun modifyResource(projectId: String, pipelineId: String, pipelineName: String) {
//        return
//    }
//
//    override fun deleteResource(projectId: String, pipelineId: String) {
//        return
//    }
//
//    override fun isProjectUser(userId: String, projectId: String, group: BkAuthGroup?): Boolean {
//        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
//            userId = userId,
//            token = checkTokenService.getSystemToken(null) ?: "",
//            action = "",
//            projectCode = projectId,
//            resourceCode = null
//        ).data ?: false
//    }
//
//    private fun getProjectAllInstance(projectId: String): List<String> {
//        return pipelineInfoDao.searchByPipelineName(dslContext, projectId)?.map { it.pipelineId } ?: emptyList()
//    }
//}
