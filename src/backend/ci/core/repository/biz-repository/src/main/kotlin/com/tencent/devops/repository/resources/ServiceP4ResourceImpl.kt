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

package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ServiceP4Resource
import com.tencent.devops.repository.service.scm.Ip4Service
import com.tencent.devops.scm.code.p4.api.P4ChangeList
import com.tencent.devops.scm.code.p4.api.P4FileSpec
import com.tencent.devops.scm.code.p4.api.P4ServerInfo
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceP4ResourceImpl @Autowired constructor(
    private val p4Service: Ip4Service
) : ServiceP4Resource {

    override fun getChangelistFiles(
        projectId: String,
        repositoryId: String,
        repositoryType: RepositoryType?,
        change: Int
    ): Result<List<P4FileSpec>> {
        return Result(
            p4Service.getChangelistFiles(
                projectId = projectId,
                repositoryId = repositoryId,
                repositoryType = repositoryType,
                change = change
            )
        )
    }

    override fun getShelvedFiles(
        projectId: String,
        repositoryId: String,
        repositoryType: RepositoryType?,
        change: Int
    ): Result<List<P4FileSpec>> {
        return Result(
            p4Service.getShelvedFiles(
                projectId = projectId,
                repositoryId = repositoryId,
                repositoryType = repositoryType,
                change = change
            )
        )
    }

    override fun getFileContent(
        p4Port: String,
        filePath: String,
        reversion: Int,
        username: String,
        password: String
    ): Result<String> {
        return Result(
            p4Service.getFileContent(
                p4Port = p4Port,
                filePath = filePath,
                reversion = reversion,
                username = username,
                password = password
            )
        )
    }

    override fun getServerInfo(
        projectId: String,
        repositoryId: String,
        repositoryType: RepositoryType?
    ): Result<P4ServerInfo> {
        return Result(
            p4Service.getServerInfo(
                projectId = projectId,
                repositoryId = repositoryId,
                repositoryType = repositoryType
            )
        )
    }

    override fun getChangelist(
        projectId: String,
        repositoryId: String,
        repositoryType: RepositoryType?,
        change: Int
    ): Result<P4ChangeList> {
        return Result(
            p4Service.getChangelist(
                projectId = projectId,
                repositoryId = repositoryId,
                repositoryType = repositoryType,
                change = change
            )
        )
    }

    override fun getShelvedChangeList(
        projectId: String,
        repositoryId: String,
        repositoryType: RepositoryType?,
        change: Int
    ): Result<P4ChangeList> {
        return Result(
            p4Service.getShelvedChangeList(
                projectId = projectId,
                repositoryId = repositoryId,
                repositoryType = repositoryType,
                change = change
            )
        )
    }
}
