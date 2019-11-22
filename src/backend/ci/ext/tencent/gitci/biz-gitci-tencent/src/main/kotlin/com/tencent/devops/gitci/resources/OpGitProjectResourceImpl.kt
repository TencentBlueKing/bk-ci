package com.tencent.devops.gitci.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.OpGitProjectResource
import com.tencent.devops.gitci.pojo.GitProjectConfWithPage
import com.tencent.devops.gitci.service.GitProjectConfService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpGitProjectResourceImpl @Autowired constructor(private val gitProjectConfService: GitProjectConfService) : OpGitProjectResource {

    override fun create(gitProjectId: Long, name: String, url: String, enable: Boolean): Result<Boolean> {
        return Result(gitProjectConfService.create(gitProjectId, name, url, enable))
    }

    override fun delete(gitProjectId: Long): Result<Boolean> {
        return Result(gitProjectConfService.delete(gitProjectId))
    }

    override fun list(gitProjectId: Long?, name: String?, url: String?, page: Int, pageSize: Int): Result<GitProjectConfWithPage> {
        return Result(GitProjectConfWithPage(gitProjectConfService.count(gitProjectId, name, url), gitProjectConfService.list(gitProjectId, name, url, page, pageSize)))
    }

    override fun update(gitProjectId: Long, name: String?, url: String?, enable: Boolean?): Result<Boolean> {
        return Result(gitProjectConfService.update(gitProjectId, name, url, enable))
    }
}
