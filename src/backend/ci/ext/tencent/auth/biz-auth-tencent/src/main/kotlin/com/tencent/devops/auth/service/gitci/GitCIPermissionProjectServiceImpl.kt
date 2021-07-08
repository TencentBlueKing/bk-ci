package com.tencent.devops.auth.service.gitci

import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.auth.utils.GitCIUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.scm.api.ServiceGitCiResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class GitCIPermissionProjectServiceImpl @Autowired constructor(
    val client: Client,
    val projectInfoService: GitCiProjectInfoService
) : PermissionProjectService {

    // GitCI权限场景不会出现次调用, 故做默认实现
    override fun getProjectUsers(projectCode: String, group: BkAuthGroup?): List<String> {
        return emptyList()
    }

    override fun getProjectGroupAndUserList(projectCode: String): List<BkAuthGroupAndUserList> {
        return emptyList()
    }

    override fun getUserProjects(userId: String): List<String> {
        return emptyList()
    }

    override fun isProjectUser(userId: String, projectCode: String, group: BkAuthGroup?): Boolean {
        val gitProjectId = GitCIUtils.getGitCiProjectId(projectCode)

        // 判断是否为开源项目
        if (projectInfoService.checkProjectPublic(gitProjectId)) {
            return true
        }

        val gitUserId = projectInfoService.getGitUserByRtx(userId, gitProjectId)
        if (gitUserId.isNullOrEmpty()) {
            GitCIPermissionServiceImpl.logger.warn("$userId is not gitCI user")
            return false
        }

        val checkResult = client.getScm(ServiceGitCiResource::class)
            .checkUserGitAuth(gitUserId, gitProjectId).data ?: false
        if (!checkResult) {
            logger.warn("$projectCode $userId is project check fail")
        }
        return checkResult
    }

    override fun createProjectUser(userId: String, projectCode: String, role: String): Boolean {
        return true
    }

    override fun getProjectRoles(projectCode: String, projectId: String): List<BKAuthProjectRolesResources> {
        return emptyList()
    }

    companion object {
        val logger = LoggerFactory.getLogger(GitCIPermissionProjectServiceImpl::class.java)
    }
}
