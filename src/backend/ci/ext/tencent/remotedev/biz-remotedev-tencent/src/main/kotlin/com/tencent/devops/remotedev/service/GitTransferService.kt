/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.RemoteDevRepository
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.repository.pojo.git.GitUserInfo
import com.tencent.devops.repository.pojo.oauth.GitToken
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

    /**
     * 获得用户oauth
     */
    fun getAndCheckOauthToken(userId: String): String

    /**
     * 获取用户git信息
     */
    fun getUserInfo(userId: String): GitUserInfo
}
