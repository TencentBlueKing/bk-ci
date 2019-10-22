package com.tencent.devops.project.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.api.OpProjectLabelResource
import com.tencent.devops.project.pojo.label.ProjectLabel
import com.tencent.devops.project.pojo.label.ProjectLabelRequest
import com.tencent.devops.project.service.ProjectLabelService

import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpProjectLabelResourceImpl @Autowired constructor(
    private val projectLabelService: ProjectLabelService
) : OpProjectLabelResource {

    override fun getProjectLabelByProjectId(projectId: String): Result<List<ProjectLabel>> {
        return Result(projectLabelService.getProjectLabelByProjectId(projectId))
    }

    override fun getAllProjectLabel(): Result<List<ProjectLabel>> {
        return Result(projectLabelService.getAllProjectLabel())
    }

    override fun getProjectLabel(id: String): Result<ProjectLabel?> {
        return Result(projectLabelService.getProjectLabel(id))
    }

    override fun addProjectLabel(projectLabelRequest: ProjectLabelRequest): Result<Boolean> {
        return Result(projectLabelService.saveProjectLabel(projectLabelRequest.labelName))
    }

    override fun updateProjectLabel(id: String, projectLabelRequest: ProjectLabelRequest): Result<Boolean> {
        return Result(projectLabelService.updateProjectLabel(id, projectLabelRequest.labelName))
    }

    override fun deleteProjectLabel(id: String): Result<Boolean> {
        return Result(projectLabelService.deleteProjectLabel(id))
    }
}
