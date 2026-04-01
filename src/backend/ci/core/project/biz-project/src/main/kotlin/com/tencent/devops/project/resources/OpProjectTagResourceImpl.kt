package com.tencent.devops.project.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.op.OpProjectTagResource
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.project.pojo.ProjectClusterPercentageResult
import com.tencent.devops.project.pojo.ProjectExtSystemTagDTO
import com.tencent.devops.project.pojo.ProjectPercentageRoutingRequest
import com.tencent.devops.project.pojo.ProjectPercentageRoutingResult
import com.tencent.devops.project.pojo.ProjectRoutingListRequest
import com.tencent.devops.project.pojo.ProjectTagUpdateDTO
import com.tencent.devops.project.service.ProjectTagService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpProjectTagResourceImpl @Autowired constructor(
    val projectTagService: ProjectTagService
) : OpProjectTagResource {

    override fun setTagByProject(projectTagUpdateDTO: ProjectTagUpdateDTO): Result<Boolean> {
        return projectTagService.updateTagByProject(projectTagUpdateDTO)
    }

    override fun setTagByOrg(projectTagUpdateDTO: ProjectTagUpdateDTO): Result<Boolean> {
        return projectTagService.updateTagByOrg(projectTagUpdateDTO)
    }

    override fun setTagByChannel(projectTagUpdateDTO: ProjectTagUpdateDTO): Result<Boolean> {
        return projectTagService.updateTagByChannel(projectTagUpdateDTO)
    }

    override fun setExtSystemTagByProject(extSystemTagDTO: ProjectExtSystemTagDTO): Result<Boolean> {
        return projectTagService.updateExtSystemRouterTag(extSystemTagDTO)
    }

    override fun setTagByPercentage(
        request: ProjectPercentageRoutingRequest
    ): Result<ProjectPercentageRoutingResult> {
        return Result(projectTagService.percentageRouting(request))
    }

    override fun addToBlacklist(request: ProjectRoutingListRequest): Result<Long> {
        return Result(projectTagService.addToBlacklist(request.projectCodes))
    }

    override fun removeFromBlacklist(request: ProjectRoutingListRequest): Result<Long> {
        return Result(projectTagService.removeFromBlacklist(request.projectCodes))
    }

    override fun getBlacklist(): Result<Set<String>> {
        return Result(projectTagService.getBlacklist())
    }

    override fun setDefaultTag(tag: String): Result<Boolean> {
        return Result(projectTagService.setDefaultTag(tag))
    }

    override fun getDefaultTag(): Result<String> {
        return Result(projectTagService.getDefaultTag())
    }

    override fun getClusterPercentage(
        condition: ProjectConditionDTO
    ): Result<ProjectClusterPercentageResult> {
        return Result(projectTagService.getClusterPercentage(condition))
    }
}
