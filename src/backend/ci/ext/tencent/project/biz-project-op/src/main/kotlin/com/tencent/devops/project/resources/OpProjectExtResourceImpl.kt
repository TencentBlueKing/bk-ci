package com.tencent.devops.project.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.op.OpProjectExtResource
import com.tencent.devops.project.pojo.OpPreProjectMigrateRequest
import com.tencent.devops.project.service.ProjectLocalService

@RestResource
@Suppress("UNUSED_PARAMETER")
class OpProjectExtResourceImpl(
    private val projectLocalService: ProjectLocalService
) : OpProjectExtResource {

    override fun migratePreProjectsForAllStoredNormalUsers(): Result<Int> {
        return Result(projectLocalService.batchGetOrCreatePreProjectsForStoredUsers())
    }

    override fun migratePreProjectsByUserIds(request: OpPreProjectMigrateRequest): Result<Int> {
        return Result(projectLocalService.batchGetOrCreatePreProjectsForUserIds(request.userIds))
    }
}
