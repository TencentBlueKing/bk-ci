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

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.BSAuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.WetestAuthServiceCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.UserWetestEmailGroupResource
import com.tencent.devops.plugin.pojo.wetest.WetestEmailGroup
import com.tencent.devops.plugin.pojo.wetest.WetestEmailGroupParam
import com.tencent.devops.plugin.pojo.wetest.WetestEmailGroupResponse
import com.tencent.devops.plugin.pojo.wetest.WetestReportResponse
import com.tencent.devops.plugin.service.WetestEmailGroupService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class UserWetestEmailGroupResourceImpl @Autowired constructor(
    private val wetestEmailGroupService: WetestEmailGroupService,
    private val bkAuthPermissionApi: BSAuthPermissionApi,
    private val serviceCode: WetestAuthServiceCode
) : UserWetestEmailGroupResource {
    override fun getWetestReport(userId: String, projectId: String): Result<WetestReportResponse?> {
        checkParam(userId, projectId)
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, AuthResourceType.WETEST_EMAIL_GROUP,
                        projectId, AuthPermission.EDIT)) {
            logger.info("用户($userId)无权限在工程($projectId)下编辑邮件组")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下编辑邮件组")
        }
        return Result(wetestEmailGroupService.getUserEmailGroup(projectId, userId))
    }

    override fun create(userId: String, projectId: String, weTestEmailGroup: WetestEmailGroupParam): Result<Map<String, Int>> {
        checkParam(userId, projectId)
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, AuthResourceType.WETEST_EMAIL_GROUP,
                        projectId, AuthPermission.CREATE)) {
            logger.info("用户($userId)无权限在工程($projectId)下创建邮件组")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下创建邮件组")
        }
        logger.info("create wetest email group, userId: $userId, projectId: $projectId")
        val record = wetestEmailGroupService.getByName(projectId, weTestEmailGroup.name)
        if (null != record) {
            logger.info("名称(${weTestEmailGroup.name})已经存在")
            throw CustomException(Response.Status.BAD_REQUEST, "创建失败，名称(${weTestEmailGroup.name})已经存在")
        }
        val id = weTestEmailGroup.run {
            wetestEmailGroupService.createWetestEmailGroup(projectId, name, userInternal, qqExternal, description, wetestGroupId, wetestGroupName)
        }
        return Result(mapOf(Pair("id", id)))
    }

    override fun update(userId: String, projectId: String, id: Int, weTestEmailGroup: WetestEmailGroupParam): Result<Boolean> {
        checkParam(userId, projectId)
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, AuthResourceType.WETEST_EMAIL_GROUP,
                        projectId, AuthPermission.EDIT)) {
            logger.info("用户($userId)无权限在工程($projectId)下编辑邮件组")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下编辑邮件组")
        }
        logger.info("update wetest email group, userId: $userId, projectId: $projectId, id: $id")
        if (null == wetestEmailGroupService.getWetestEmailGroup(projectId, id)) {
            logger.info("记录不存在")
            throw CustomException(Response.Status.BAD_REQUEST, "编辑失败，记录不存在")
        }
        val record = wetestEmailGroupService.getByName(projectId, weTestEmailGroup.name)
        if (null != record && record.id != id) {
            logger.info("名称(${weTestEmailGroup.name})已经存在")
            throw CustomException(Response.Status.BAD_REQUEST, "编辑失败，名称(${weTestEmailGroup.name})已经存在")
        }

        weTestEmailGroup.run {
            wetestEmailGroupService.updateWetestEmailGroup(projectId, id, name, userInternal, qqExternal, description, wetestGroupId, wetestGroupName)
        }
        return Result(true)
    }

    override fun get(userId: String, projectId: String, id: Int): Result<WetestEmailGroup?> {
        checkParam(userId, projectId)
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, AuthResourceType.WETEST_EMAIL_GROUP,
                        projectId, AuthPermission.VIEW)) {
            logger.info("用户($userId)无权限在工程($projectId)下查询邮件组")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下查询邮件组")
        }
        val record = wetestEmailGroupService.getWetestEmailGroup(projectId, id)
        return if (null == record) {
            Result(1, "记录不存在")
        } else {
            Result(record)
        }
    }

    override fun getList(userId: String, projectId: String, page: Int, pageSize: Int): Result<WetestEmailGroupResponse?> {
        checkParam(userId, projectId)
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, AuthResourceType.WETEST_EMAIL_GROUP,
                        projectId, AuthPermission.VIEW)) {
            logger.info("用户($userId)无权限在工程($projectId)下查询邮件组")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下查询邮件组")
        }

        val resultList = wetestEmailGroupService.getList(projectId, page, pageSize)
        val resultCount = wetestEmailGroupService.getCount(projectId)
        return Result(data = WetestEmailGroupResponse(
                count = resultCount.toString(),
                page = page,
                pageSize = pageSize,
                totalPages = resultCount / pageSize + 1,
                records = resultList
        ))
    }

    override fun delete(userId: String, projectId: String, id: Int): Result<Boolean> {
        checkParam(userId, projectId)
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, AuthResourceType.WETEST_EMAIL_GROUP,
                        projectId, AuthPermission.DELETE)) {
            logger.info("用户($userId)无权限在工程($projectId)下删除邮件组")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下删除邮件组")
        }

        logger.info("delete wetest email group, userId: $userId, id: $id")
        wetestEmailGroupService.deleteWetestEmailGroup(projectId, id)
        return Result(true)
    }

    fun checkParam(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserWetestEmailGroupResourceImpl::class.java)
    }
}
