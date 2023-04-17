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

package com.tencent.devops.project.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.service.ServiceProjectTagResource
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.service.ProjectService
import com.tencent.devops.project.service.ProjectTagService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceProjectTagResourceImpl @Autowired constructor(
    val projectTagService: ProjectTagService,
    val projectService: ProjectService
) : ServiceProjectTagResource {
    override fun checkProjectRouter(projectId: String): Result<Boolean> {
        return Result(projectTagService.checkProjectTag(projectId))
    }

    override fun isRbacPermission(projectId: String): Result<Boolean> {
        return Result(projectService.isRbacPermission(projectId))
    }

    override fun updateProjectRouteTag(projectCode: String, tag: String): Result<Boolean> {
        return Result(projectTagService.updateTagByProject(projectCode = projectCode, tag = tag))
    }
}
