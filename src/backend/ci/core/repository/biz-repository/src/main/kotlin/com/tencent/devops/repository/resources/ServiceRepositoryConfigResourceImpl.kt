package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ServiceRepositoryConfigResource
import com.tencent.devops.repository.pojo.RepositoryScmConfigSummary
import com.tencent.devops.repository.service.RepositoryScmConfigService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceRepositoryConfigResourceImpl @Autowired constructor(
    private val repositoryScmConfigService: RepositoryScmConfigService
) : ServiceRepositoryConfigResource {

    override fun getSummary(scmCode: String): Result<RepositoryScmConfigSummary> {
        val summary = repositoryScmConfigService.get(scmCode).let {
            RepositoryScmConfigSummary(
                scmCode = it.scmCode,
                name = it.name,
                providerCode = it.providerCode,
                scmType = it.scmType
            )
        }
        return Result(summary)
    }
}
