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

package com.tencent.devops.stream.trigger.git.service

import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitCred
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitChangeFileInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitCommitInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitCred
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitFileInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitMrChangeInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitMrInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitProjectInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitProjectUserInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitTreeFileInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitUserInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TGitApiService @Autowired constructor() : StreamGitApiService {

    /**
     * 通过凭据获取可以直接使用的token
     */
    override fun getToken(
        cred: StreamGitCred,
    ): String {
        TODO("Not yet implemented")
    }

    override fun getGitProjectInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        retry: ApiRequestRetryInfo
    ): TGitProjectInfo? {
        cred as TGitCred
        TODO("Not yet implemented")
    }

    override fun getGitCommitInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        sha: String,
        retry: ApiRequestRetryInfo
    ): TGitCommitInfo? {
        TODO("Not yet implemented")
    }

    override fun getUserInfoByToken(cred: StreamGitCred): TGitUserInfo? {
        TODO("Not yet implemented")
    }

    override fun getProjectUserInfo(
        cred: StreamGitCred,
        userId: String,
        gitProjectId: String
    ): TGitProjectUserInfo {
        TODO("Not yet implemented")
    }

    override fun getMrInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        mrId: String,
        retry: ApiRequestRetryInfo
    ): TGitMrInfo? {
        TODO("Not yet implemented")
    }

    override fun getMrChangeInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        mrId: String,
        retry: ApiRequestRetryInfo
    ): TGitMrChangeInfo? {
        TODO("Not yet implemented")
    }

    override fun getFileTree(
        cred: StreamGitCred,
        gitProjectId: String,
        path: String?,
        ref: String?,
        recursive: Boolean,
        retry: ApiRequestRetryInfo
    ): List<TGitTreeFileInfo> {
        TODO("Not yet implemented")
    }

    override fun getFileContent(
        cred: StreamGitCred,
        gitProjectId: String,
        fileName: String,
        ref: String?,
        retry: ApiRequestRetryInfo
    ): String {
        TODO("Not yet implemented")
    }

    override fun getFileInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        fileName: String,
        ref: String?,
        retry: ApiRequestRetryInfo
    ): TGitFileInfo? {
        TODO("Not yet implemented")
    }

    /**
     * 获取两个commit之间的差异文件
     * @param from 旧commit
     * @param to 新commit
     * @param straight true：两个点比较差异，false：三个点比较差异。默认是 false
     */
    fun getCommitChangeList(
        cred: TGitCred,
        gitProjectId: String,
        from: String,
        to: String,
        straight: Boolean,
        page: Int,
        pageSize: Int,
        retry: ApiRequestRetryInfo
    ): List<TGitChangeFileInfo> {
        TODO("Not yet implemented")
    }

    /**
     * 为mr添加评论
     */
    fun addMrComment(
        cred: TGitCred,
        gitProjectId: String,
        mrId: Long,
        mrBody: String
    ) {
        TODO()
    }
}
