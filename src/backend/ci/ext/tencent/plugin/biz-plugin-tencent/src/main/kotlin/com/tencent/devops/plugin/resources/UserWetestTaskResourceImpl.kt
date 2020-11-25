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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.BSAuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.WetestAuthServiceCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.UserWetestTaskResource
import com.tencent.devops.plugin.pojo.wetest.WetestTask
import com.tencent.devops.plugin.pojo.wetest.WetestTaskParam
import com.tencent.devops.plugin.pojo.wetest.WetestTaskResponse
import com.tencent.devops.plugin.pojo.wetest.WetestCloud
import com.tencent.devops.plugin.pojo.wetest.WetestTestType
import com.tencent.devops.plugin.service.WetestTaskService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class UserWetestTaskResourceImpl @Autowired constructor(
    private val wetestTaskService: WetestTaskService,
    private val bkAuthPermissionApi: BSAuthPermissionApi,
    private val objectMapper: ObjectMapper,
    private val serviceCode: WetestAuthServiceCode
) : UserWetestTaskResource {
    override fun create(userId: String, projectId: String, wetestTaskParam: WetestTaskParam): Result<Map<String, Int>> {
        checkParam(userId, projectId)
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, AuthResourceType.WETEST_TASK,
                        projectId, AuthPermission.CREATE)) {
            logger.info("用户($userId)无权限在工程($projectId)下创建WeTest测试任务")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下创建WeTest测试任务")
        }
        logger.info("create weTest task, userId: $userId, projectId: $projectId")
        val record = wetestTaskService.getByName(projectId, wetestTaskParam.name)
        if (null != record) {
            logger.info("名称(${wetestTaskParam.name})已经存在")
            throw CustomException(Response.Status.BAD_REQUEST, "创建失败，名称(${wetestTaskParam.name})已经存在")
        }
        val id = wetestTaskService.createTask(projectId, wetestTaskParam.name, wetestTaskParam.mobileCategory, wetestTaskParam.mobileCategoryId,
                wetestTaskParam.mobileModel, wetestTaskParam.mobileModelId, wetestTaskParam.description)
        return Result(mapOf(Pair("id", id)))
    }

    override fun update(userId: String, projectId: String, id: Int, wetestTaskParam: WetestTaskParam): Result<Boolean> {
        checkParam(userId, projectId)
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, AuthResourceType.WETEST_TASK,
                        projectId, AuthPermission.EDIT)) {
            logger.info("用户($userId)无权限在工程($projectId)下编辑WeTest测试任务")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下编辑WeTest测试任务")
        }
        logger.info("update wetest email group, userId: $userId, id: $id, projectId: $projectId")
        if (null == wetestTaskService.getTask(projectId, id)) {
            logger.info("记录不存在")
            throw CustomException(Response.Status.BAD_REQUEST, "编辑失败，记录不存在")
        }
        val record = wetestTaskService.getByName(projectId, wetestTaskParam.name)
        if (null != record && record.id != id) {
            logger.info("名称(${wetestTaskParam.name})已经存在")
            throw CustomException(Response.Status.BAD_REQUEST, "编辑失败，名称(${wetestTaskParam.name})已经存在")
        }

        wetestTaskService.updateTask(projectId, id, wetestTaskParam.name, wetestTaskParam.mobileCategory, wetestTaskParam.mobileCategoryId,
                wetestTaskParam.mobileModel, wetestTaskParam.mobileModelId, wetestTaskParam.description)
        return Result(true)
    }

    override fun get(userId: String, projectId: String, id: Int): Result<WetestTask?> {
        checkParam(userId, projectId)
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, AuthResourceType.WETEST_TASK,
                        projectId, AuthPermission.VIEW)) {
            logger.info("用户($userId)无权限在工程($projectId)下查询WeTest测试任务")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下查询WeTest测试任务")
        }
        val record = wetestTaskService.getTask(projectId, id)
        return if (null == record) {
            Result(1, "记录不存在")
        } else {
            Result(record)
        }
    }

    override fun getList(userId: String, projectId: String, page: Int, pageSize: Int): Result<WetestTaskResponse?> {
        checkParam(userId, projectId)
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, AuthResourceType.WETEST_TASK,
                        projectId, AuthPermission.VIEW)) {
            logger.info("用户($userId)无权限在工程($projectId)下查询WeTest测试任务")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下查询WeTest测试任务")
        }

        val resultList = wetestTaskService.getList(projectId, page, pageSize)
        val resultCount = wetestTaskService.getCount(projectId)
        return Result(data = WetestTaskResponse(
                count = resultCount.toString(),
                page = page,
                pageSize = pageSize,
                totalPages = resultCount / pageSize + 1,
                records = resultList
        ))
    }

    override fun delete(userId: String, projectId: String, id: Int): Result<Boolean> {
        checkParam(userId, projectId)
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, AuthResourceType.WETEST_TASK,
                        projectId, AuthPermission.DELETE)) {
            logger.info("用户($userId)无权限在工程($projectId)下删除WeTest测试任务")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下删除WeTest测试任务")
        }

        logger.info("delete weTest task, userId: $userId, id: $id")
        wetestTaskService.deleteTask(projectId, id)
        return Result(true)
    }

    override fun getMyCloud(userId: String, projectId: String): Result<Map<String, String>?> {
        checkParam(userId, projectId)
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, AuthResourceType.WETEST_TASK,
                        projectId, AuthPermission.VIEW)) {
            logger.info("用户($userId)无权限在工程($projectId)下查询WeTest测试任务")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下查询WeTest测试任务")
        }
        logger.info("getMyCloud from weTest, userId: $userId, projectId: $projectId")
        return Result(wetestTaskService.getMyCloud(userId, projectId))
    }

    override fun getPrivateCloudDevice(userId: String, projectId: String, cloudIds: String, online: String?, free: String?): Result<List<WetestCloud>> {
        checkParam(userId, projectId)
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, AuthResourceType.WETEST_TASK,
                        projectId, AuthPermission.VIEW)) {
            logger.info("用户($userId)无权限在工程($projectId)下查询WeTest测试任务")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下查询WeTest测试任务")
        }
        logger.info("getPrivateCloudDevice from weTest, userId: $userId, projectId: $projectId, cloudIds: $cloudIds")
        return Result(wetestTaskService.getPrivateCloudDevice(userId, projectId, cloudIds, online ?: "1", free ?: "0"))
    }

    override fun getPrivateTestInfo(userId: String, projectId: String, testId: String, createUser: String): Result<Map<String, Any>> {
        checkParam(userId, projectId)
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, AuthResourceType.WETEST_TASK,
                        projectId, AuthPermission.VIEW)) {
            logger.info("用户($userId)无权限在工程($projectId)下查询WeTest测试任务")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下查询WeTest测试任务")
        }
        logger.info("getPrivateTestInfo from weTest, userId: $userId, projectId: $projectId, testId: $testId")
        return Result(objectMapper.readValue<Map<String, Any>>(wetestTaskService.getPrivateTestInfo(createUser, projectId, testId).toString()))
    }

    override fun getTestDevicePerfError(userId: String, projectId: String, testId: String, deviceId: String, needPerf: String?, needError: String?, createUser: String): Result<Map<String, Any>> {
        checkParam(userId, projectId)
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, AuthResourceType.WETEST_TASK,
                        projectId, AuthPermission.VIEW)) {
            logger.info("用户($userId)无权限在工程($projectId)下查询WeTest测试任务")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下查询WeTest测试任务")
        }
        logger.info("getTestDevicePerfError from weTest, userId: $userId, projectId: $projectId, testId: $testId," +
                "deviceId: $deviceId, needPerf: $needPerf, needError: $needError")
        return Result(objectMapper.readValue<Map<String, Any>>(wetestTaskService.getTestDevicePerfError(createUser, projectId, testId, deviceId, needPerf ?: "1", needError ?: "1").toString()))
    }

    override fun getTestDeviceImageLog(userId: String, projectId: String, testId: String, deviceId: String, needImage: String?, needLog: String?, createUser: String): Result<Map<String, Any>> {
        checkParam(userId, projectId)
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, AuthResourceType.WETEST_TASK,
                        projectId, AuthPermission.VIEW)) {
            logger.info("用户($userId)无权限在工程($projectId)下查询WeTest测试任务")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下查询WeTest测试任务")
        }
        logger.info("getTestDeviceImageLog from weTest, userId: $userId, projectId: $projectId, testId: $testId," +
                "deviceId: $deviceId, needPerf: $needImage, needLog: $needLog")
        return Result(objectMapper.readValue<Map<String, Any>>(wetestTaskService.getTestDeviceImageLog(createUser, projectId, testId, deviceId, needImage ?: "1", needLog ?: "1").toString()))
    }

    override fun getTestTypeScriptType(userId: String, projectId: String, createUser: String): Result<List<WetestTestType>> {
        checkParam(userId, projectId)
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, AuthResourceType.WETEST_TASK,
                        projectId, AuthPermission.VIEW)) {
            logger.info("用户($userId)无权限在工程($projectId)下查询WeTest测试任务")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下查询WeTest测试任务")
        }
        logger.info("getTestTypeScriptType from weTest, userId: $userId, projectId: $projectId")
        return Result(wetestTaskService.getTestTypeScriptType(createUser, projectId))
    }

    override fun getTestLogContent(userId: String, projectId: String, testId: String, deviceId: String, level: String?, startLine: Int?, lineCnt: Int?, createUser: String): Result<Map<String, Any>> {
        checkParam(userId, projectId)
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, AuthResourceType.WETEST_TASK,
                        projectId, AuthPermission.VIEW)) {
            logger.info("用户($userId)无权限在工程($projectId)下查询WeTest测试任务")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下查询WeTest测试任务")
        }
        logger.info("getTestLogContent from weTest, userId: $userId, projectId: $projectId")
        return Result(objectMapper.readValue<Map<String, Any>>(wetestTaskService.getTestLogContent(createUser, projectId, testId, deviceId, level
                ?: "all", startLine ?: 1, lineCnt ?: 200).toString()))
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
        private val logger = LoggerFactory.getLogger(UserWetestTaskResourceImpl::class.java)
    }
}