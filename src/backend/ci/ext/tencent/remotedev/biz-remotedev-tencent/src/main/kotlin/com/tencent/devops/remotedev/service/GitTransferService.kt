package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.RemoteDevRepository
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
        refreshToken: Boolean?
    ): Result<AuthorizeResult>

    fun getProjectList(
        userId: String,
        page: Int,
        pageSize: Int,
        search: String?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<RemoteDevRepository>

    fun getProjectBranches(
        userId: String,
        pathWithNamespace: String,
        page: Int?,
        pageSize: Int?,
        search: String?
    ): List<String>?

    /**
     * 获取yaml文件的具体内容
     * @param filePath 文件路径
     */
    fun getFileContent(
        userId: String,
        pathWithNamespace: String,
        filePath: String,
        ref: String
    ): String

    /**
     * 获取Git仓库文件列表
     * @param path 获取文件路径下的文件列表
     * @param ref commit hash值、分支 或 tag
     * @param recursive 是否支持递归目录结构
     */
    fun getFileNameTree(
        userId: String,
        pathWithNamespace: String,
        path: String?,
        ref: String?,
        recursive: Boolean
    ): List<String>
}
