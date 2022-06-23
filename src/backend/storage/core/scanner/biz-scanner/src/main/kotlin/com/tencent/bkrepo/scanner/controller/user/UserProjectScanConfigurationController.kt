/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.scanner.controller.user

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.scanner.pojo.ProjectScanConfiguration
import com.tencent.bkrepo.scanner.pojo.request.ProjectScanConfigurationPageRequest
import com.tencent.bkrepo.scanner.service.ProjectScanConfigurationService
import io.swagger.annotations.Api
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Api("项目扫描配置接口")
@RestController
@RequestMapping("/api/scan/configurations")
@Principal(PrincipalType.ADMIN)
class UserProjectScanConfigurationController(
    private val projectScanConfigurationService: ProjectScanConfigurationService
) {

    @PostMapping
    fun create(@RequestBody request: ProjectScanConfiguration): Response<ProjectScanConfiguration> {
        val configuration = projectScanConfigurationService.create(request)
        return ResponseBuilder.success(configuration)
    }


    @PutMapping
    fun update(@RequestBody request: ProjectScanConfiguration): Response<ProjectScanConfiguration> {
        val configuration = projectScanConfigurationService.update(request)
        return ResponseBuilder.success(configuration)
    }

    @GetMapping
    fun page(request: ProjectScanConfigurationPageRequest): Response<Page<ProjectScanConfiguration>> {
        val page = projectScanConfigurationService.page(request)
        return ResponseBuilder.success(page)
    }

    @GetMapping("/{projectId}")
    fun get(@PathVariable projectId: String): Response<ProjectScanConfiguration> {
        return ResponseBuilder.success(projectScanConfigurationService.get(projectId))
    }
}
