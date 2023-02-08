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
 *
 */

package com.tencent.devops.project.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.BkAuthProperties
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.listener.TxIamV3CreateEvent
import com.tencent.devops.project.pojo.ApplicationInfo
import com.tencent.devops.project.pojo.AuthProjectCreateInfo
import com.tencent.devops.project.pojo.AuthProjectForCreateResult
import com.tencent.devops.project.pojo.ResourceUpdateInfo
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.ProjectPermissionService
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

class TxV3ProjectPermissionServiceImpl @Autowired constructor(
    val objectMapper: ObjectMapper,
    val authProperties: BkAuthProperties,
    val projectDispatcher: ProjectDispatcher,
    val client: Client,
    val tokenService: ClientTokenService
) : ProjectPermissionService {

    @Value("\${iam.v0.url:#{null}}")
    private val v0IamUrl: String = ""

    // 校验用户是否是项目成员
    override fun verifyUserProjectPermission(accessToken: String?, projectCode: String, userId: String): Boolean {
        return client.get(ServiceProjectAuthResource::class).isProjectUser(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectCode,
            group = null
        ).data
            ?: false
    }

    override fun createResources(
        resourceRegisterInfo: ResourceRegisterInfo,
        resourceCreateInfo: AuthProjectCreateInfo
    ): String {
        val userId = resourceCreateInfo.userId
        val accessToken = resourceCreateInfo.accessToken
        val userDeptDetail = resourceCreateInfo.userDeptDetail
        // TODO: (V3创建项目未完全迁移完前，需双写V0,V3)
        // 同步创建V0项目
        val projectId = createResourcesToV0(userId, accessToken, resourceRegisterInfo, userDeptDetail)
        // 异步创建V3项目
        projectDispatcher.dispatch(
            TxIamV3CreateEvent(
                userId = userId,
                retryCount = 0,
                delayMills = 1000,
                resourceRegisterInfo = resourceRegisterInfo,
                projectId = projectId,
                iamProjectId = null
            )
        )
        return projectId
    }

    override fun deleteResource(projectCode: String) {
        // 资源都在接入方本地，无需删除iam侧数据
        return
    }

    override fun modifyResource(
        resourceUpdateInfo: ResourceUpdateInfo
    ) {
        // 资源都在接入方本地，无需修改iam侧数据
        return
    }

    override fun getUserProjects(userId: String): List<String> {
        return client.get(ServiceProjectAuthResource::class).getUserProjects(
            token = tokenService.getSystemToken(null)!!,
            userId = userId
        ).data ?: emptyList()
    }

    override fun getUserProjectsAvailable(userId: String): Map<String, String> {
        // TODO:
        return emptyMap()
    }

    override fun cancelCreateAuthProject(projectCode: String) = Unit

    override fun cancelUpdateAuthProject(projectCode: String) = Unit

    override fun createRoleGroupApplication(
        userId: String,
        applicationInfo: ApplicationInfo,
        gradeManagerId: String
    ): Boolean {
        return true
    }

    override fun needApproval(needApproval: Boolean?) = false

    override fun verifyUserProjectPermission(
        accessToken: String?,
        projectCode: String,
        userId: String,
        permission: AuthPermission
    ): Boolean {
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectCode,
            resourceCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            action = permission.value,
            relationResourceType = null
        ).data ?: false
    }

    fun createResourcesToV0(
        userId: String,
        accessToken: String?,
        projectCreateInfo: ResourceRegisterInfo,
        userDeptDetail: UserDeptDetail?
    ): String {
        // 创建AUTH项目
        val authUrl = "$v0IamUrl/projects?access_token=$accessToken"
        val param: MutableMap<String, String> = mutableMapOf("project_code" to projectCreateInfo.resourceCode)
        if (userDeptDetail != null) {
            param["bg_id"] = userDeptDetail.bgId
            param["dept_id"] = userDeptDetail.deptId
            param["center_id"] = userDeptDetail.centerId
            logger.info("createProjectResources add org info $param")
        }
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val json = objectMapper.writeValueAsString(param)
        val requestBody = RequestBody.create(mediaType, json)
        val request = Request.Builder().url(authUrl).post(requestBody).build()
        val responseContent = request(request, "调用权限中心创建项目失败")
        val result = objectMapper.readValue<Result<AuthProjectForCreateResult>>(responseContent)
        if (result.isNotOk()) {
            logger.warn("Fail to create the project of response $responseContent")
            throw OperationException("调用权限中心V0创建项目失败: ${result.message}")
        }
        val authProjectForCreateResult = result.data
        return if (authProjectForCreateResult != null) {
            if (authProjectForCreateResult.project_id.isBlank()) {
                throw OperationException("权限中心创建V0的项目ID无效")
            }
            authProjectForCreateResult.project_id
        } else {
            logger.warn("Fail to get the project id from response $responseContent")
            throw OperationException("权限中心V0创建的项目ID无效")
        }
    }

    private fun request(request: Request, errorMessage: String): String {
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn(
                    "Fail to request($request) with code ${response.code()} , " +
                        "message ${response.message()} and response $responseContent"
                )
                throw OperationException(errorMessage)
            }
            return responseContent
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(TxV3ProjectPermissionServiceImpl::class.java)
    }
}
