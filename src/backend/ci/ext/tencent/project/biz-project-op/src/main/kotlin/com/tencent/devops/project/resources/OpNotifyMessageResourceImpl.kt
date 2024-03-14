package com.tencent.devops.project.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.op.OpNotifyMessageResource
import com.tencent.devops.project.pojo.SendEmailForProjectByConditionDTO
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

    override fun sendEmailForRelatedObsByCondition(
        sendEmailForProjectByConditionDTO: SendEmailForProjectByConditionDTO
    ): Result<Boolean> {
        return Result(
            projectNotifyService.sendEmailForRelatedObsByCondition(
                sendEmailForProjectByConditionDTO = sendEmailForProjectByConditionDTO
            )
        )
    }

    override fun getProjectsForRelatedObsByCondition(
        sendEmailForProjectByConditionDTO: SendEmailForProjectByConditionDTO
    ): Result<Pair<Int, List<String>>> {
        return Result(
            projectNotifyService.getProjectsForRelatedObsByCondition(
                sendEmailForProjectByConditionDTO = sendEmailForProjectByConditionDTO
            )
        )
    }

    override fun sendEmailForProjectOrganizationChange(): Result<Boolean> {
        return Result(
            projectNotifyService.sendEmailForProjectOrganizationChange()
        )
    }

    override fun sendEmailForProjectProductChange(): Result<Boolean> {
        return Result(
            projectNotifyService.sendEmailForProjectProductChange()
        )
    }

    override fun sendEmailForVerifyProjectOrganization(): Result<Boolean> {
        return Result(
            projectNotifyService.sendEmailForVerifyProjectOrganization()
        )
    }
}
