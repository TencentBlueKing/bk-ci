package com.tencent.devops.repository.resources

import com.tencent.devops.repository.api.UserOauthResource
import com.tencent.devops.repository.pojo.OauthRepositoryResource
import com.tencent.devops.repository.pojo.OauthResetUrl
import com.tencent.devops.repository.pojo.UserOauthRepositoryInfo
import com.tencent.devops.common.api.enums.ScmCode
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
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
        scmCode: ScmCode,
        page: Int?,
        pageSize: Int?
    ): Result<Page<OauthRepositoryResource>> {
        val targetPage = page ?: 1
        val targetPageSize = pageSize ?: 20
        val resources = oauthRepositoryService.relSource(
            userId = userId,
            scmCode = scmCode,
            page = targetPage,
            pageSize = targetPageSize
        ).let { pageInfo ->
            Page(
                records = pageInfo.records.map {
                    OauthRepositoryResource(
                        name = it.aliasName,
                        url = it.detailUrl ?: ""
                    )
                },
                count = pageInfo.count,
                page = pageInfo.page,
                pageSize = targetPageSize
            )
        }
        return Result(resources)
    }

    override fun delete(
        userId: String,
        scmCode: ScmCode
    ): Result<Boolean> {
        oauthRepositoryService.delete(
            userId = userId,
            scmCode = scmCode
        )
        return Result(true)
    }

    override fun reset(
        userId: String,
        scmCode: ScmCode,
        redirectUrl: String
    ): Result<OauthResetUrl> {
        return Result(
            oauthRepositoryService.reset(
                userId = userId,
                scmCode = scmCode,
                redirectUrl = redirectUrl
            )
        )
    }
}
