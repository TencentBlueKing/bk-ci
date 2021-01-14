package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ServiceAuthRepositoryResource
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.service.RepositoryService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceAuthRepositoryResourceImpl @Autowired constructor(
    private val repositoryService: RepositoryService
) : ServiceAuthRepositoryResource {

    override fun listByProjects(projectId: String, offset: Int?, limit: Int?): Result<Page<RepositoryInfo>> {
        val result = repositoryService.listByProject(setOf(projectId), null, offset!!, limit!!)
        return Result(Page(limit!!, offset!!, result.count, result.records))
    }

    override fun getInfos(repositoryIds: List<String>): Result<List<RepositoryInfo>?> {
        return Result(repositoryService.getInfoByHashIds(repositoryIds))
    }
}