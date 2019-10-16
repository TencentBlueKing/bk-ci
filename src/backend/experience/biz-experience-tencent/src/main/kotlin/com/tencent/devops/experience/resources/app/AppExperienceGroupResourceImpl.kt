package com.tencent.devops.experience.resources.app

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.app.AppExperienceGroupResource
import com.tencent.devops.experience.pojo.GroupSummaryWithPermission
import com.tencent.devops.experience.service.GroupService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AppExperienceGroupResourceImpl @Autowired constructor(
    private val groupService: GroupService
) : AppExperienceGroupResource {
    override fun list(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<GroupSummaryWithPermission>> {
        val SQLLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        val result = groupService.list(userId, projectId, SQLLimit.offset, SQLLimit.limit)
        return Result(Page(SQLLimit.offset, SQLLimit.limit, result.first, result.second))
    }
}