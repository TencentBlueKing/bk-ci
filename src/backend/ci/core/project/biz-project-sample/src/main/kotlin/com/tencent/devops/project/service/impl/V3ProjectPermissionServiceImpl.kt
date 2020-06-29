package com.tencent.devops.project.service.impl

import com.tencent.devops.auth.api.ServiceGroupResource
import com.tencent.devops.auth.pojo.dto.GroupDTO
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.service.ProjectPermissionService
import org.springframework.beans.factory.annotation.Autowired

class V3ProjectPermissionServiceImpl @Autowired constructor(
    val client: Client
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
        // 创建从属于该项目的默认内置用户组CI管理员,用户拉入用户组
        val initProjectGroup = client.get(ServiceGroupResource::class).createGroup(
            userId = userId,
            addCreateUser = true,
            projectCode = resourceRegisterInfo.resourceCode,
            groupInfo = GroupDTO(
                groupCode = BkAuthGroup.CIADMIN.value,
                groupType = 1,
                groupName = BkAuthGroup.CIADMIN.name,
                authPermissionList = emptyList()
            )
        )
        if (initProjectGroup.isNotOk()) {
            // 添加用户组失败抛异常
            throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PEM_CREATE_FAIL))
        }
        return ""
    }

    override fun deleteResource(projectCode: String) {
        TODO("Not yet implemented")
    }

    override fun modifyResource(projectCode: String, projectName: String) {
        TODO("Not yet implemented")
    }

    override fun getUserProjects(userId: String): List<String> {
        TODO("Not yet implemented")
    }

    override fun getUserProjectsAvailable(userId: String): Map<String, String> {
        TODO("Not yet implemented")
    }
}