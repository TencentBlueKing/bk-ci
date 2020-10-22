package com.tencent.devops.project.service.impl

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.ProjectPermissionService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class V3ProjectPermissionServiceImpl @Autowired constructor(
    val client: Client,
    private val authProjectApi: AuthProjectApi,
    private val authResourceApi: AuthResourceApi,
    private val authPermissionApi: AuthPermissionApi,
    private val projectAuthServiceCode: ProjectAuthServiceCode,
    private val projectDao: ProjectDao,
    private val dslContext: DSLContext
) : ProjectPermissionService {

    override fun verifyUserProjectPermission(accessToken: String?, projectCode: String, userId: String): Boolean {
        return authProjectApi.isProjectUser(
            user = userId,
            serviceCode = projectAuthServiceCode,
            projectCode = projectCode,
            group = null
        )
    }

    // 创建项目
    override fun createResources(
        userId: String,
        accessToken: String?,
        resourceRegisterInfo: ResourceRegisterInfo,
        userDeptDetail: UserDeptDetail?
    ): String {
        val validateCreatePermission = authPermissionApi.validateUserResourcePermission(userId, projectAuthServiceCode, AuthResourceType.PROJECT, "", AuthPermission.CREATE)
        if (!validateCreatePermission) {
            throw PermissionForbiddenException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.USER_NOT_CREATE_PERM))
        }
        authResourceApi.createResource(userId, projectAuthServiceCode, AuthResourceType.PROJECT, resourceRegisterInfo.resourceCode, resourceRegisterInfo.resourceCode, resourceRegisterInfo.resourceName)
        return ""
    }

    override fun deleteResource(projectCode: String) {
        return
    }

    override fun modifyResource(projectCode: String, projectName: String) {
        return
    }

    override fun getUserProjects(userId: String): List<String> {
        val projects = authProjectApi.getUserProjects(
            serviceCode = projectAuthServiceCode,
            userId = userId,
            supplier = null
        )

        if (projects == null || projects.isEmpty()) {
            return emptyList()
        }

        val projectList = mutableListOf<String>()
        return if (projects[0] == "*") {
            projectDao.getAllProject(dslContext).filter { projectList.add(it.englishName) }
            projectList
        } else {
            projects.map {
                projectList.add(it.trim())
            }
            projectList
        }
    }

    override fun getUserProjectsAvailable(userId: String): Map<String, String> {
        return authProjectApi.getUserProjectsAvailable(
            userId = userId,
            serviceCode = projectAuthServiceCode,
            supplier = null
        )
    }

    override fun verifyUserProjectPermission(accessToken: String?, projectCode: String, userId: String, permission: AuthPermission): Boolean {
        return authPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = projectAuthServiceCode,
                resourceType = projectResourceType,
                resourceCode = projectCode,
                projectCode = projectCode,
                permission = permission
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
        val projectResourceType = AuthResourceType.PROJECT
    }
}