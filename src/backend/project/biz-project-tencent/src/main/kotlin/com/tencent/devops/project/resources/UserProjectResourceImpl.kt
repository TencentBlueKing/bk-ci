package com.tencent.devops.project.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.service.user.UserProjectResource
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import com.tencent.devops.project.service.ProjectService
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream

@RestResource
class UserProjectResourceImpl @Autowired constructor(private val projectService: ProjectService) : UserProjectResource {

    override fun list(accessToken: String, includeDisable: Boolean?): Result<List<ProjectVO>> {
        return Result(projectService.list(accessToken, includeDisable))
    }

    override fun get(accessToken: String, english_name: String): Result<ProjectVO> {
        return Result(projectService.getByEnglishName(accessToken, english_name))
    }

    override fun create(userId: String, accessToken: String, projectCreateInfo: ProjectCreateInfo): Result<Boolean> {
        // 创建蓝盾项目
        projectService.create(userId, accessToken, projectCreateInfo)

        return Result(true)
    }

    override fun update(
        userId: String,
        accessToken: String,
        projectId: String,
        projectUpdateInfo: ProjectUpdateInfo
    ): Result<Boolean> {
        projectService.update(userId, accessToken, projectId, projectUpdateInfo)
        return Result(true)
    }

    override fun enable(
        userId: String,
        accessToken: String,
        projectId: String,
        enabled: Boolean
    ): Result<Boolean> {
        projectService.updateUsableStatus(userId, projectId, enabled)
        return Result(true)
    }

    override fun updateLogo(
        userId: String,
        accessToken: String,
        projectId: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): Result<Boolean> {
        return projectService.updateLogo(userId, accessToken, projectId, inputStream, disposition)
    }

    override fun validate(
        userId: String,
        validateType: ProjectValidateType,
        name: String,
        project_id: String?
    ): Result<Boolean> {
        projectService.validate(validateType, name, project_id)
        return Result(true)
    }

    override fun getV2(accessToken: String, english_name: String): Result<ProjectVO> {
        return Result(projectService.getByEnglishName(accessToken, english_name))
    }

    override fun updateV2(userId: String, accessToken: String, projectId: String, projectUpdateInfo: ProjectUpdateInfo): Result<Boolean> {
        projectService.update(userId, accessToken, projectId, projectUpdateInfo)
        return Result(true)
    }

    override fun enableV2(userId: String, accessToken: String, projectId: String, enabled: Boolean): Result<Boolean> {
        projectService.updateUsableStatus(userId, projectId, enabled)
        return Result(true)
    }

    override fun updateLogoV2(userId: String, accessToken: String, projectId: String, inputStream: InputStream, disposition: FormDataContentDisposition): Result<Boolean> {
        return projectService.updateLogo(userId, accessToken, projectId, inputStream, disposition)
    }

    override fun validateV2(userId: String, validateType: ProjectValidateType, name: String, project_id: String?): Result<Boolean> {
        projectService.validate(validateType, name, project_id)
        return Result(true)
    }
}
