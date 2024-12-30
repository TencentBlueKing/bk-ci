package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.enums.ScmCode
import com.tencent.devops.repository.api.UserOauthResource
import com.tencent.devops.repository.pojo.OauthResetUrl
import com.tencent.devops.repository.pojo.UserOauthRepositoryInfo
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.pojo.RepoOauthRefVo
import com.tencent.devops.repository.service.OauthRepositoryService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserOauthResourceImpl @Autowired constructor(
    val oauthRepositoryService: OauthRepositoryService
) : UserOauthResource {
    override fun list(userId: String): Result<List<UserOauthRepositoryInfo>> {
        return Result(oauthRepositoryService.list(userId))
    }

    override fun relSource(
        userId: String,
        scmCode: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<RepoOauthRefVo>> {
        val targetPage = page ?: 1
        val targetPageSize = pageSize ?: 20
        val resources = oauthRepositoryService.relSource(
            userId = userId,
            scmCode = ScmCode.valueOf(scmCode),
            page = targetPage,
            pageSize = targetPageSize
        )
        return Result(resources)
    }

    override fun delete(
        userId: String,
        scmCode: String
    ): Result<Boolean> {
        oauthRepositoryService.delete(
            userId = userId,
            scmCode = ScmCode.valueOf(scmCode)
        )
        return Result(true)
    }

    override fun reset(
        userId: String,
        scmCode: String,
        redirectUrl: String
    ): Result<OauthResetUrl> {
        return Result(
            oauthRepositoryService.reset(
                userId = userId,
                scmCode = ScmCode.valueOf(scmCode),
                redirectUrl = redirectUrl
            )
        )
    }
}
