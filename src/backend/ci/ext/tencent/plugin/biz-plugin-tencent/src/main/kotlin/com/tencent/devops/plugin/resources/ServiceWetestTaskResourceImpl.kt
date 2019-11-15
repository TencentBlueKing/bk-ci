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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.ServiceWetestTaskResource
import com.tencent.devops.plugin.pojo.wetest.WetestAutoTestRequest
import com.tencent.devops.plugin.pojo.wetest.WetestInstStatus
import com.tencent.devops.plugin.pojo.wetest.WetestTask
import com.tencent.devops.plugin.pojo.wetest.WetestTaskInst
import com.tencent.devops.plugin.service.WetestService
import com.tencent.devops.plugin.service.WetestTaskInstService
import com.tencent.devops.plugin.service.WetestTaskService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceWetestTaskResourceImpl @Autowired constructor(
    private val wetestTaskService: WetestTaskService,
    private val wetestTaskInstService: WetestTaskInstService,
    private val wetestService: WetestService
) : ServiceWetestTaskResource {
    override fun getTask(taskId: String, projectId: String): Result<WetestTask?> {
        return Result(wetestTaskService.getTask(projectId, taskId.toInt()))
    }

    override fun saveTaskInst(wetestTaskInst: WetestTaskInst): Result<String> {
        return Result(wetestTaskInstService.saveTask(wetestTaskInst))
    }

    override fun updateTaskInstStatus(testId: String, status: WetestInstStatus): Result<String> {
        return Result(wetestTaskInstService.updateTaskInstStatus(testId, status))
    }

    override fun uploadRes(accessId: String, accessToken: String, type: String, fileParams: ArtifactorySearchParam): Result<Map<String, Any>> {
        return Result(wetestService.uploadRes(accessId, accessToken, type, fileParams))
    }

    override fun autoTest(accessId: String, accessToken: String, request: WetestAutoTestRequest): Result<Map<String, Any>> {
        return Result(wetestService.autoTest(accessId, accessToken, request))
    }

    override fun queryTestStatus(accessId: String, accessToken: String, testId: String): Result<Map<String, Any>> {
        return Result(wetestService.queryTestStatus(accessId, accessToken, testId))
    }
}