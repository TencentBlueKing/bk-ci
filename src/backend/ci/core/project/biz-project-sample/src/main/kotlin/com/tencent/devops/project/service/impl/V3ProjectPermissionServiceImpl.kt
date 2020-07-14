package com.tencent.devops.project.service.impl


import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.service.ProjectPermissionService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class V3ProjectPermissionServiceImpl @Autowired constructor(
    val client: Client,
    private val authProjectApi: AuthProjectApi,
    private val authPermissionApi: AuthPermissionApi,
    private val projectAuthServiceCode: ProjectAuthServiceCode,
    private val projectDao: ProjectDao,
    private val dslContext: DSLContext
) : ProjectPermissionService {

    override fun verifyUserProjectPermission(accessToken: String?, projectCode: String, userId: String): Boolean {
        TODO("Not yet implemented")
    }

    // 创建项目
    override fun createResources(
        userId: String,
        accessToken: String?,
        resourceRegisterInfo: ResourceRegisterInfo
    ): String {
//        // 创建从属于该项目的默认内置用户组CI管理员,用户拉入用户组
//        val initProjectGroup = client.get(ServiceGroupResource::class).createGroup(
//            userId = userId,
//            projectCode = resourceRegisterInfo.resourceCode,
//            groupInfo = GroupDTO(
//                groupCode = BkAuthGroup.CIADMIN.value,
//                groupType = GroupType.DEFAULT,
//                groupName = BkAuthGroup.CIADMIN.name,
//                authPermissionList = emptyList()
//            )
//        )
//        if (initProjectGroup.isNotOk() || initProjectGroup.data.isNullOrEmpty()) {
//            // 添加用户组失败抛异常
//            throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PEM_CREATE_FAIL))
//        }
//        val groupId = initProjectGroup.data
//        client.get(ServiceUserGroupResource::class).addUser2Group(userId, groupId!!)

        return ""
    }

    override fun deleteResource(projectCode: String) {
        TODO("Not yet implemented")
    }

    override fun modifyResource(projectCode: String, projectName: String) {
        TODO("Not yet implemented")
    }

    override fun getUserProjects(userId: String): List<String> {
        val projects = authProjectApi.getUserProjects(
            serviceCode = projectAuthServiceCode,
            userId = userId,
            supplier = null
        )
        val projectList = mutableListOf<String>()
        return if(projects[0] == "*") {
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
        TODO("Not yet implemented")
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}