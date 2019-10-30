package com.tencent.devops.project.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.service.ServiceTxProjectResource
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.service.ProjectLocalService
import com.tencent.devops.project.service.ProjectPermissionService
import com.tencent.devops.project.service.ProjectService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceTxProjectResourceImpl @Autowired constructor(
        private val projectPermissionService: ProjectPermissionService,
        private val projectLocalService: ProjectLocalService
) : ServiceTxProjectResource {
    override fun getProjectEnNamesByOrganization(
        userId: String,
        bgId: Long,
        deptName: String?,
        centerName: String?
    ): Result<List<String>> {
        return Result(
             projectLocalService.getProjectEnNamesByOrganization(
                userId = userId,
                bgId = bgId,
                deptName = deptName,
                centerName = centerName,
                interfaceName = "/service/projects/enNames/organization"
            )
        )
    }

    override fun getProjectByGroup(
        userId: String,
        bgName: String?,
        deptName: String?,
        centerName: String?
    ): Result<List<ProjectVO>> {
        return Result(projectLocalService.getProjectByGroup(userId, bgName, deptName, centerName))
    }

    override fun list(accessToken: String): Result<List<ProjectVO>> {
        return Result(projectLocalService.list(accessToken, true))
    }


    override fun getPreUserProject(userId: String, accessToken: String): Result<ProjectVO?> {
        return Result(projectLocalService.getOrCreatePreProject(userId, accessToken))
    }


    override fun getPreUserProjectV2(userId: String, accessToken: String): Result<ProjectVO?> {
        return Result(projectLocalService.getOrCreatePreProject(userId, accessToken))
    }

    //TODO
    override fun create(userId: String, projectCreateInfo: ProjectCreateInfo): Result<String> {
        return Result(projectLocalService.create(userId, "", projectCreateInfo))
    }
}
