package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.BuildTgitGroupStatResource
import com.tencent.devops.plugin.api.pojo.GitGroupStatRequest
import com.tencent.devops.plugin.service.TgitGroupStatService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildTgitGroupStatResourceImpl @Autowired constructor(
    private val tgitGroupStatService: TgitGroupStatService
) : BuildTgitGroupStatResource {
    override fun reportGitGroupStat(group: String, gitGroupStatRequest: GitGroupStatRequest): Result<Boolean> {
        return tgitGroupStatService.reportGitGroupStat(group, gitGroupStatRequest)
    }
}