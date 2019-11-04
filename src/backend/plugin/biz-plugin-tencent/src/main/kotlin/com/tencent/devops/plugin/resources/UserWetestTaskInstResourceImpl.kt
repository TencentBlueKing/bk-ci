/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.BSAuthPermissionApi
import com.tencent.devops.common.auth.code.WetestAuthServiceCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.UserWetestTaskInstResource
import com.tencent.devops.plugin.pojo.wetest.WetestTaskInstReport
import com.tencent.devops.plugin.service.WetestTaskInstService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserWetestTaskInstResourceImpl @Autowired constructor(
    private val wetestTaskInstService: WetestTaskInstService,
    private val bkAuthPermissionApi: BSAuthPermissionApi,
    private val serviceCode: WetestAuthServiceCode
) : UserWetestTaskInstResource {

    companion object {
        private val logger = LoggerFactory.getLogger(UserWetestTaskInstResourceImpl::class.java)
    }

    override fun list(request: UserWetestTaskInstResource.ListRequest?, userId: String, projectId: String, page: Int?, pageSize: Int?): Result<Page<WetestTaskInstReport>> {
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, AuthResourceType.WETEST_TASK,
                        projectId, AuthPermission.VIEW)) {
            logger.info("用户($userId)无权限在工程($projectId)下查看WeTest测试任务")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下查看WeTest测试任务")
        }
        return Result(wetestTaskInstService.getTaskInstReportByPage(projectId, request?.pipelineIds, request?.versions, page, pageSize))
    }

    override fun listByBuildId(userId: String, pipelineId: String?, buildId: String?, projectId: String, page: Int?, pageSize: Int?): Result<Page<WetestTaskInstReport>> {
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, AuthResourceType.WETEST_TASK,
                        projectId, AuthPermission.VIEW)) {
            logger.info("用户($userId)无权限在工程($projectId)下查看WeTest测试任务")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下查看WeTest测试任务")
        }
        return Result(wetestTaskInstService.getTaskInstReportByBuildId(projectId, pipelineId, buildId, page, pageSize))
    }

    override fun listVersion(pipelineId: Set<String>?, userId: String, projectId: String): Result<Set<String>> {
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, AuthResourceType.WETEST_TASK,
                        projectId, AuthPermission.VIEW)) {
            logger.info("用户($userId)无权限在工程($projectId)下查看WeTest测试任务版本")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下查看WeTest测试任务版本")
        }
        return Result(wetestTaskInstService.getTaskInstVersion(projectId, pipelineId))
    }

    override fun listPipeline(userId: String, projectId: String, version: Set<String>?): Result<List<UserWetestTaskInstResource.PipelineData>> {
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, AuthResourceType.WETEST_TASK,
                        projectId, AuthPermission.VIEW)) {
            logger.info("用户($userId)无权限在工程($projectId)下查看WeTest测试任务流水线")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下查看WeTest测试任务流水线")
        }
        return Result(wetestTaskInstService.listPipeline(projectId, version))
    }

    override fun getSession(userId: String, projectId: String): Result<Map<String, Any>> {
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, AuthResourceType.WETEST_TASK,
                        projectId, AuthPermission.VIEW)) {
            logger.info("用户($userId)无权限在工程($projectId)下查看WeTest测试任务流水线")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下查看WeTest测试任务流水线")
        }
        return Result(wetestTaskInstService.getSession(userId))
    }
}