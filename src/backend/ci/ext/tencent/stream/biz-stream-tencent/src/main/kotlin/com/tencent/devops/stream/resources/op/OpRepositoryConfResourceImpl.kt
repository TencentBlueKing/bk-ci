package com.tencent.devops.stream.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.stream.api.op.OpRepositoryConfResource
import com.tencent.devops.stream.service.TXStreamRepositoryConfService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpRepositoryConfResourceImpl @Autowired constructor(
    private val tXStreamRepositoryConfService: TXStreamRepositoryConfService
) : OpRepositoryConfResource {

    // 更新repo conf中记录的旧域名
    override fun updateRepoConfGitDomain(
        oldGitDomain: String,
        newGitDomain: String,
        limitNumber: Int
    ): Result<Boolean> {
        return Result(
            tXStreamRepositoryConfService.updateGitDomain(
                oldGitDomain = oldGitDomain,
                newGitDomain = newGitDomain,
                limitNumber = limitNumber
            )
        )
    }
}
