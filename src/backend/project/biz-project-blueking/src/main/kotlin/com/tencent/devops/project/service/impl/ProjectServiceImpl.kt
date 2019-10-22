package com.tencent.devops.project.service.impl

import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.jmx.api.ProjectJmxApi
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.UserRole
import com.tencent.devops.project.service.ProjectPermissionService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectServiceImpl @Autowired constructor(
        private val projectPermissionService: ProjectPermissionService,
        private val dslContext: DSLContext,
        private val projectDao: ProjectDao,
        private val projectJmxApi: ProjectJmxApi,
        private val redisOperation: RedisOperation,
        private val gray: Gray,
        private val client: Client
): AbsProjectServiceImpl(projectPermissionService, dslContext, projectDao, projectJmxApi, redisOperation, gray, client){
    override fun create(userId: String, accessToken: String, projectCreateInfo: ProjectCreateInfo) {
        return
    }

    override fun getProjectEnNamesByOrganization(userId: String, bgId: Long?, deptName: String?, centerName: String?, interfaceName: String?): List<String> {
        return emptyList()
    }

    override fun getOrCreatePreProject(userId: String, accessToken: String): ProjectVO {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getProjectByGroup(userId: String, bgName: String?, deptName: String?, centerName: String?): List<ProjectVO> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateUsableStatus(userId: String, projectId: String, enabled: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getProjectUsers(accessToken: String, userId: String, projectCode: String): Result<List<String>?> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getProjectUserRoles(accessToken: String, userId: String, projectCode: String, serviceCode: AuthServiceCode): List<UserRole> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}