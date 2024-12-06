package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.user.UserOauthResource
import com.tencent.devops.auth.pojo.OauthRelResource
import com.tencent.devops.auth.pojo.OauthResetUrl
import com.tencent.devops.auth.pojo.UserOauthInfo
import com.tencent.devops.auth.pojo.enum.OauthType
import com.tencent.devops.auth.service.UserOauthService
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserOauthResourceImpl @Autowired constructor(
    val userOauthService: UserOauthService
) : UserOauthResource {
    override fun list(userId: String, projectId: String?): Result<List<UserOauthInfo>> {
        return Result(
            userOauthService.list(
                userId = userId,
                projectId = projectId
            )
        )
    }

    override fun relSource(
        userId: String,
        projectId: String?,
        oauthType: OauthType,
        page: Int?,
        pageSize: Int?
    ): Result<Page<OauthRelResource>> {
        return Result(
            userOauthService.relSource(
                userId = userId,
                projectId = projectId,
                oauthType = oauthType,
                page = page ?: 1,
                pageSize = pageSize ?: 20
            )
        )
    }

    override fun delete(
        userId: String,
        projectId: String?,
        oauthType: OauthType
    ): Result<Boolean> {
        userOauthService.delete(
            userId = userId,
            projectId = projectId,
            oauthType = oauthType
        )
        return Result(true)
    }

    override fun reOauth(
        userId: String,
        oauthType: OauthType,
        redirectUrl: String
    ): Result<OauthResetUrl> {
        return Result(
            userOauthService.reOauth(
                userId = userId,
                oauthType = oauthType,
                redirectUrl = redirectUrl
            )
        )
    }
}
