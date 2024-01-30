package com.tencent.devops.project.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.op.OpNotifyMessageResource
import com.tencent.devops.project.service.ProjectNotifyService

@RestResource
class OpNotifyMessageResourceImpl constructor(
    val projectNotifyService: ProjectNotifyService
) : OpNotifyMessageResource {
    override fun sendEmailToUserForRelatedObsByProjectIds(projectIds: List<String>): Result<Boolean> {
        return Result(
            projectNotifyService.sendEmailForRelatedObsByProjectIds(
                projectIds = projectIds
            )
        )
    }

    override fun sendEmailForRelatedObsByBgId(bgId: Long): Result<Boolean> {
        return Result(
            projectNotifyService.sendEmailForRelatedObsByBgId(
                bgId = bgId
            )
        )
    }

    override fun getProjectsForRelatedObsByBgId(bgId: Long): Result<Pair<Int, Map<String, List<String>>>> {
        return Result(
            projectNotifyService.getProjectsForRelatedObsByBgId(
                bgId = bgId
            )
        )
    }
}
