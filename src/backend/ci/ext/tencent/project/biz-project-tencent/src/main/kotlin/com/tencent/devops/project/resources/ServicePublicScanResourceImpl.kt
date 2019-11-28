package com.tencent.devops.project.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.service.service.ServicePublicScanResource
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.service.ProjectS3Service
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServicePublicScanResourceImpl @Autowired constructor(
    private val projectS3Service: ProjectS3Service
) : ServicePublicScanResource {

    override fun createCodeCCScanProject(
        userId: String,
        projectCreateInfo: ProjectCreateInfo
    ): Result<ProjectVO> {
        return Result(projectS3Service.createCodeCCScanProject(userId, projectCreateInfo))
    }
}