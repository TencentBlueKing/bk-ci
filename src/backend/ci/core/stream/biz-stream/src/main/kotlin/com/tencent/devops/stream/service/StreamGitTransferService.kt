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

package com.tencent.devops.stream.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.AppInstallationResult
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.stream.pojo.StreamCommitInfo
import com.tencent.devops.stream.pojo.StreamCreateFileInfo
import com.tencent.devops.stream.pojo.StreamGitGroup
import com.tencent.devops.stream.pojo.StreamGitMember
import com.tencent.devops.stream.pojo.StreamGitProjectBaseInfoCache
import com.tencent.devops.stream.pojo.StreamGitProjectInfoWithProject
import com.tencent.devops.stream.pojo.StreamProjectGitInfo
import com.tencent.devops.stream.pojo.enums.StreamBranchesOrder
import com.tencent.devops.stream.pojo.enums.StreamProjectsOrder
import com.tencent.devops.stream.pojo.enums.StreamSortAscOrDesc

/**
 * 将后台中非trigger的部分需要多个Git端调用的依据配置的不同注入不同的bean
 */
interface StreamGitTransferService {

    /**
     * 获取缓存需要的项目信息
     * @see com.tencent.devops.stream.service.StreamGitProjectInfoCache.getAndSaveGitProjectInfo
     */
    fun getGitProjectCache(
        gitProjectId: String,
        useAccessToken: Boolean,
        userId: String?,
        accessToken: String? = null
    ): StreamGitProjectBaseInfoCache

    /**
     * 获取项目信息
     */
    fun getGitProjectInfo(
        gitProjectId: String,
        userId: String?
    ): StreamGitProjectInfoWithProject?

    /**
     * 获取yaml的具体内容
     */
    fun getYamlContent(
        gitProjectId: String,
        userId: String,
        fileName: String,
        ref: String
    ): String

    /**
     * 获取项目列表
     */
    fun getProjectList(
        userId: String,
        page: Int,
        pageSize: Int,
        search: String?,
        orderBy: StreamProjectsOrder?,
        sort: StreamSortAscOrDesc?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<StreamProjectGitInfo>?

    /**
     * 获取项目成员
     */
    fun getProjectMember(
        gitProjectId: String,
        userId: String,
        page: Int?,
        pageSize: Int?,
        search: String?
    ): List<StreamGitMember>

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

    /**
     * 主动开启git侧 ci
     */
    fun enableCi(
        userId: String,
        projectName: String,
        enable: Boolean? = true
    ): Result<Boolean>

    /**
     * 获取当前项目的提交记录
     */
    fun getCommits(
        userId: String,
        gitProjectId: Long,
        filePath: String?,
        branch: String?,
        since: String?,
        until: String?,
        page: Int?,
        perPage: Int?
    ): List<StreamCommitInfo>?

    /**
     * 在当前Git项目创建新文件
     */
    fun createNewFile(
        userId: String,
        gitProjectId: String,
        streamCreateFile: StreamCreateFileInfo
    ): Boolean

    /**
     * 获取当前项目下的分支列表
     */
    fun getProjectBranches(
        userId: String,
        gitProjectId: String,
        page: Int?,
        pageSize: Int?,
        search: String?,
        orderBy: StreamBranchesOrder?,
        sort: StreamSortAscOrDesc?
    ): List<String>?

    /**
     * 获取用户的项目组列表
     */
    fun getProjectGroupsList(
        userId: String,
        page: Int,
        pageSize: Int
    ): List<StreamGitGroup>?

    /**
     * app是否安装
     */
    fun isInstallApp(
        userId: String,
        gitProjectId: Long
    ): AppInstallationResult
}
