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

package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.pojo.EnvWithPermission
import com.tencent.devops.plugin.api.BuildEnvironmentResource
import com.tencent.devops.plugin.service.JobService
import org.springframework.beans.factory.annotation.Autowired

/**
 * @Description 为插件【作业平台-执行脚本】提供build接口
 * @author Jsonwan
 * @Date 2019/8/15
 * @version 1.0
 */

@RestResource
class BuildEnvironmentResourceImpl @Autowired constructor(
    val jobService: JobService
) : BuildEnvironmentResource {

    override fun listUsableServerEnvs(projectId: String, buildId: String): Result<List<EnvWithPermission>> {
        return jobService.listUsableServerEnvs(projectId, buildId)
    }

    override fun listRawByEnvHashIds(projectId: String, buildId: String, envHashIds: List<String>): Result<List<EnvWithPermission>> {
        return jobService.listRawByEnvHashIds(projectId, buildId, envHashIds)
    }

    override fun listRawByEnvNames(projectId: String, buildId: String, envNames: List<String>): Result<List<EnvWithPermission>> {
        return jobService.listRawByEnvNames(projectId, buildId, envNames)
    }

    override fun listUsableServerEnvsByLastUpdateUser(
        projectId: String,
        pipelineId: String
    ): Result<List<EnvWithPermission>> {
        return jobService.listUsableServerEnvsByLastUpdateUser(projectId, pipelineId)
    }
}
