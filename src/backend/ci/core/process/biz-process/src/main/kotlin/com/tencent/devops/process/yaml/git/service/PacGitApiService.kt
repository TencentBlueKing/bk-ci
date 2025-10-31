/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.yaml.git.service

import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.process.yaml.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.process.yaml.git.pojo.PacGitCommitInfo
import com.tencent.devops.process.yaml.git.pojo.PacGitCred
import com.tencent.devops.process.yaml.git.pojo.PacGitFileInfo
import com.tencent.devops.process.yaml.git.pojo.PacGitMrChangeInfo
import com.tencent.devops.process.yaml.git.pojo.PacGitMrInfo
import com.tencent.devops.process.yaml.git.pojo.PacGitProjectInfo
import com.tencent.devops.process.yaml.git.pojo.PacGitPushResult
import com.tencent.devops.process.yaml.git.pojo.PacGitTreeFileInfo

/**
 * PAC 需要用到的各平台的标准接口
 * 注：在代码主流程中直接使用api的方法需要放到这里，只在各个action中的可以在其具体的GitApiService中去实现
 * 注2：只应该在trigger相关逻辑中使用此接口，其他地方使用scm相关
 */
interface PacGitApiService {

    /**
     * 通过传递的凭据信息直接获取实体token，方便直接调用接口，类似commit check之类的
     * @param cred 调用api相关凭证
     */
    fun getToken(cred: PacGitCred): String

    /**
     * 获取stream需要的git项目信息
     * 当前api加入缓存，需要调用优先从缓存获取
     * @see com.tencent.devops.stream.trigger.parsers.StreamTriggerCache.getAndSaveRequestGitProjectInfo
     * @param gitProjectId git项目唯一标识
     * @param retry 当前请求重试的相关信息
     */
    fun getGitProjectInfo(
        cred: PacGitCred,
        gitProjectId: String,
        retry: ApiRequestRetryInfo
    ): PacGitProjectInfo?

    /**
     * 获取pac需要的git commit相关信息
     * @param sha 获取commit的信息，例如：hash值、分支名或tag
     * @param gitProjectId 使用关联event事件的 git project id
     */
    fun getGitCommitInfo(
        cred: PacGitCred,
        gitProjectId: String,
        sha: String,
        retry: ApiRequestRetryInfo
    ): PacGitCommitInfo?

    /**
     * 获取合并请求信息
     * @param mrId 合并请求的唯一凭证
     * @param gitProjectId 使用关联event事件的 git project id
     */
    fun getMrInfo(
        cred: PacGitCred,
        gitProjectId: String,
        mrId: String,
        retry: ApiRequestRetryInfo
    ): PacGitMrInfo?

    /**
     * 获取合并请求信息包括变更文件
     * @param gitProjectId 使用关联event事件的 git project id
     */
    fun getMrChangeInfo(
        cred: PacGitCred,
        gitProjectId: String,
        mrId: String,
        retry: ApiRequestRetryInfo
    ): PacGitMrChangeInfo?

    /**
     * 获取Git仓库文件列表
     * @param path 获取文件路径下的文件列表
     * @param ref commit hash值、分支 或 tag
     * @param recursive 是否支持递归目录结构
     */
    fun getFileTree(
        cred: PacGitCred,
        gitProjectId: String,
        path: String?,
        ref: String?,
        recursive: Boolean,
        retry: ApiRequestRetryInfo
    ): List<PacGitTreeFileInfo>

    /**
     * 获取yaml文件的具体内容
     * @param fileName 文件名称
     */
    fun getFileContent(
        cred: PacGitCred,
        gitProjectId: String,
        fileName: String,
        ref: String,
        retry: ApiRequestRetryInfo
    ): String

    /**
     * 获取yaml文件内容以及文件信息
     */
    fun getFileInfo(
        cred: PacGitCred,
        gitProjectId: String,
        fileName: String,
        ref: String?,
        retry: ApiRequestRetryInfo
    ): PacGitFileInfo?

    fun checkPushPermission(userId: String, cred: PacGitCred, gitProjectId: String, authUserId: String): Boolean

    /**
     * 提交yaml文件
     */
    fun pushYamlFile(
        userId: String,
        cred: PacGitCred,
        gitProjectId: String,
        defaultBranch: String,
        filePath: String,
        content: String,
        commitMessage: String,
        targetAction: CodeTargetAction,
        pipelineId: String,
        pipelineName: String,
        versionName: String?,
        targetBranch: String?
    ): PacGitPushResult
}
