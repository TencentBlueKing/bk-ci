package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.github.GithubAppUrl
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.scm.enums.GitAccessLevelEnum

interface GitTransferService {
    /**
     * 判断用户是否经过oauth授权
     */
    fun isOAuth(
        userId: String,
        redirectUrlType: RedirectUrlTypeEnum?,
        redirectUrl: String?,
        gitProjectId: Long,
        refreshToken: Boolean?
    ): Result<AuthorizeResult>

    fun getProjectList(
        userId: String,
        page: Int,
        pageSize: Int,
        search: String?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<GithubAppUrl>
}