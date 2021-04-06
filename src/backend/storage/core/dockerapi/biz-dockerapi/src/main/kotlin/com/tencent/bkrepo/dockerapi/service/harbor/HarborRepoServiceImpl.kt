/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.dockerapi.service.harbor

import com.tencent.bkrepo.dockerapi.client.HarborClient
import com.tencent.bkrepo.dockerapi.pojo.ImageAccount
import com.tencent.bkrepo.dockerapi.pojo.Repository
import com.tencent.bkrepo.dockerapi.service.RepoService
import com.tencent.bkrepo.dockerapi.util.AccountUtils
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.Random

@Service
@ConditionalOnProperty(prefix = "dockerapi", name = ["realm"], havingValue = "harbor")
class HarborRepoServiceImpl(
    private val harborClient: HarborClient
) : RepoService {
    override fun createRepo(projectId: String): Repository {
        logger.info("createRepo, projectId: $projectId")
        var harborProject = harborClient.getProjectByName(projectId)
        if (harborProject == null) {
            harborClient.createProject(projectId)
            harborProject = harborClient.getProjectByName(projectId)
        }
        return Repository(harborProject!!.name, "", harborProject!!.name)
    }

    override fun createAccount(projectId: String): ImageAccount {
        logger.info("createAccount, projectId: $projectId")
        val userName = System.currentTimeMillis().toString() + Random().nextInt(1000).toString()
        val password = AccountUtils.generateRandomPassword(8)

        var harborProject = harborClient.getProjectByName(projectId)
        if (harborProject == null) {
            harborClient.createProject(projectId)
            harborProject = harborClient.getProjectByName(projectId)
        }
        harborClient.createUser(userName, password)
        harborClient.addProjectMember(userName, harborProject!!.projectId)
        return ImageAccount(userName, password)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(HarborRepoServiceImpl::class.java)
    }
}
