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
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.ServiceJinGangAppResource
import com.tencent.devops.plugin.pojo.JinGangBugCount
import com.tencent.devops.plugin.service.JinGangService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceJinGangAppResourceImpl @Autowired constructor(
    private val jinGangService: JinGangService
) : ServiceJinGangAppResource {

    override fun scanApp(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildNo: Int,
        elementId: String,
        file: String,
        isCustom: Boolean,
        runType: String
    ): Result<String> {
//        if (!jinGangService.checkLimit(projectId, pipelineId)) return Result("$projectId 超过了当天运行次数了")
        return Result(jinGangService.scanApp(userId, projectId, pipelineId, buildId, buildNo, elementId, file, isCustom, runType))
    }

    override fun countBug(projectIds: Set<String>?): Result<Map<String, JinGangBugCount>> {
        return Result(jinGangService.countBug(projectIds ?: setOf()))
    }

    override fun countRisk(projectIds: Set<String>?): Result<Map<String, Int>> {
        return Result(jinGangService.countRisk(projectIds ?: setOf()))
    }
}