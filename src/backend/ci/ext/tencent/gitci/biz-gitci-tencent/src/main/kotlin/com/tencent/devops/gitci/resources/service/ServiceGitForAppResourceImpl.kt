package com.tencent.devops.gitci.resources.service

import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.service.ServiceGitForAppResource
import com.tencent.devops.gitci.v2.service.GitCIAppService
import com.tencent.devops.project.pojo.app.AppProjectVO
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceGitForAppResourceImpl @Autowired constructor(
    val gitCIAppService: GitCIAppService
) : ServiceGitForAppResource {
    override fun getGitCIProjectList(
        userId: String,
        page: Int,
        pageSize: Int,
        searchName: String?
    ): Result<Pagination<AppProjectVO>> {
        return Result(gitCIAppService.getGitCIProjectList(userId, page, pageSize, searchName))
    }
}
