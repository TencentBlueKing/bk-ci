package com.tencent.devops.project.resources

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.app.AppProjectResource
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.app.AppProjectVO
import com.tencent.devops.project.service.ProjectLocalService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AppProjectResourceImpl @Autowired constructor(
    private val projectLocalService: ProjectLocalService
) : AppProjectResource {
    override fun list(userId: String, page: Int, pageSize: Int, searchName: String?): Result<List<AppProjectVO>> {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        return Result(projectLocalService.listForApp(userId, sqlLimit.offset, sqlLimit.limit, searchName))
    }
}
