package com.tencent.devops.turbo.controller

import com.tencent.devops.common.api.exception.TurboException
import com.tencent.devops.common.api.exception.code.IS_NOT_ADMIN_MEMBER
import com.tencent.devops.common.util.constants.NO_ADMIN_MEMBER_MESSAGE
import com.tencent.devops.turbo.api.IUserCustomScheduleTaskController
import com.tencent.devops.turbo.pojo.CustomScheduleJobModel
import com.tencent.devops.turbo.service.CustomScheduleJobService
import com.tencent.devops.turbo.service.TurboAuthService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController


@RestController
class UserCustomScheduleTaskController @Autowired constructor(
    private val turboAuthService: TurboAuthService,
    private val customScheduleJobService: CustomScheduleJobService
) : IUserCustomScheduleTaskController {

    override fun addScheduleJob(
        user: String,
        projectId: String,
        customScheduleJobModel: CustomScheduleJobModel
    ): Boolean {
        if (!turboAuthService.validatePlatformMember(projectId, user)) {
            throw TurboException(errorCode = IS_NOT_ADMIN_MEMBER, errorMessage = NO_ADMIN_MEMBER_MESSAGE)
        }
        return customScheduleJobService.customScheduledJobAdd(customScheduleJobModel)
    }

    override fun triggerCustomScheduleJob(user: String, projectId: String, jobName: String): String? {
        if (!turboAuthService.getAuthResult(projectId, user)) {
            throw TurboException(errorCode = IS_NOT_ADMIN_MEMBER, errorMessage = NO_ADMIN_MEMBER_MESSAGE)
        }
        return customScheduleJobService.trigger(jobName)
    }
}
