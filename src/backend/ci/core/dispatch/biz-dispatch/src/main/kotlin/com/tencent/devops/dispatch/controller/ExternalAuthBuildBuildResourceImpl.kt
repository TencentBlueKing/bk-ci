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

package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.ExternalAuthBuildResource
import com.tencent.devops.dispatch.pojo.AuthBuildResponse
import com.tencent.devops.dispatch.service.AuthBuildService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ExternalAuthBuildBuildResourceImpl @Autowired constructor(
    private val authBuildService: AuthBuildService
) : ExternalAuthBuildResource {

    override fun authAgent(
        secretKey: String,
        agentId: String,
        buildId: String,
        vmSeqId: String?,
        token: String
    ): Result<AuthBuildResponse> {
        return Result(
            authBuildService.authAgent(
                secretKey = secretKey,
                agentId = agentId,
                buildId = buildId,
                vmSeqId = vmSeqId,
                token = token
            )
        )
    }

    override fun authDocker(
        secretKey: String,
        agentId: String,
        token: String
    ): Result<AuthBuildResponse> {
        return Result(
            authBuildService.authDocker(
                secretKey = secretKey,
                agentId = agentId,
                token = token
            )
        )
    }

    override fun authPlugin(
        secretKey: String,
        agentId: String,
        token: String
    ): Result<AuthBuildResponse> {
        return Result(
            authBuildService.authPlugin(
                secretKey = secretKey,
                agentId = agentId,
                token = token
            )
        )
    }

    override fun authMacos(
        clientIp: String?,
        checkVersion: Boolean,
        token: String
    ): Result<AuthBuildResponse> {
        return Result(
            authBuildService.authMacos(
                clientIp = clientIp,
                checkVersion = checkVersion,
                token = token
            )
        )
    }

    override fun authOther(
        clientIp: String?,
        token: String
    ): Result<AuthBuildResponse> {
        return Result(
            authBuildService.authOther(
                clientIp = clientIp,
                token = token
            )
        )
    }
}