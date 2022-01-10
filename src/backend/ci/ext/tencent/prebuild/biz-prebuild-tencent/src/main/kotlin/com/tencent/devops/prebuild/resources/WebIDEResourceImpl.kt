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

package com.tencent.devops.prebuild.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.prebuild.pojo.ide.IdeDirInfo
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStaticInfo
import com.tencent.devops.prebuild.api.WebIDEResource
import com.tencent.devops.prebuild.pojo.IDEAgentReq
import com.tencent.devops.prebuild.pojo.IDEInfo
import com.tencent.devops.prebuild.service.WebIDEService
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.project.pojo.ProjectVO
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class WebIDEResourceImpl @Autowired constructor(private val webIDEService: WebIDEService) : WebIDEResource {
    override fun updateLastOpenDir(userId: String, ip: String, path: String): Result<Boolean> {
        return Result(webIDEService.updateLastOpenDir(userId, ip, path))
    }

    override fun lastOpenDir(userId: String, ip: String): Result<IdeDirInfo> {
        return Result(webIDEService.lastOpenDir(userId, ip))
    }

    override fun heartBeat(userId: String, ip: String): Result<Boolean> {
        return Result(webIDEService.heartBeat(userId, ip))
    }

    override fun getUserProject(userId: String, accessToken: String): Result<ProjectVO?> {
        return Result(webIDEService.getUserProject(userId, accessToken))
    }

    override fun getAgentInstallLink(userId: String, projectId: String, zoneName: String, operationSystem: String, initIp: String): Result<ThirdPartyAgentStaticInfo> {
        return Result(webIDEService.getAgentInstallLink(userId, projectId, operationSystem, zoneName, initIp))
    }

    override fun getUserIDEList(userId: String, projectId: String): Result<List<IDEInfo>> {
        return Result(webIDEService.getUserIDEInfo(userId, projectId))
    }

    override fun setupAgent(userId: String, req: IDEAgentReq): Result<BuildId> {
        return Result(webIDEService.setupAgent(userId, req.projectId, req.ip))
    }
}
