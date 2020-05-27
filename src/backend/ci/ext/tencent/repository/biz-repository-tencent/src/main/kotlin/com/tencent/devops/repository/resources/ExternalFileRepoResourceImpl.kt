package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ExternalFileRepoResource
import com.tencent.devops.repository.service.scm.TencentGitCiService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ExternalFileRepoResourceImpl @Autowired constructor(
    private val gitciService: TencentGitCiService
) : ExternalFileRepoResource {
    override fun getFileContent(repoUrl: String, filePath: String, ref: String?, subModule: String?): Result<String> {
        return Result(gitciService.getFileContent(repoUrl, filePath, ref, subModule))
    }
}