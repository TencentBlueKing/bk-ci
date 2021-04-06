/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.auth.service.bkauth

import com.tencent.bkrepo.auth.config.BkAuthConfig
import com.tencent.bkrepo.auth.pojo.enums.BkAuthPermission
import com.tencent.bkrepo.auth.pojo.enums.BkAuthResourceType
import com.tencent.bkrepo.auth.pojo.enums.BkAuthServiceCode
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.repository.PermissionRepository
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.repository.UserRepository
import com.tencent.bkrepo.auth.service.local.PermissionServiceImpl
import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.repository.api.RepositoryClient
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate

/**
 * 对接蓝鲸权限中心2.x
 */
class BkAuthPermissionServiceImpl constructor(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val permissionRepository: PermissionRepository,
    private val mongoTemplate: MongoTemplate,
    private val repositoryClient: RepositoryClient,
    private val bkAuthConfig: BkAuthConfig,
    private val bkAuthService: BkAuthService,
    private val bkAuthProjectService: BkAuthProjectService
) : PermissionServiceImpl(userRepository, roleRepository, permissionRepository, mongoTemplate, repositoryClient) {
    private fun parsePipelineId(path: String): String? {
        val roads = path.split("/")
        return if (roads.size < 2 || roads[1].isBlank()) {
            logger.warn("parse pipelineId failed, path: $path")
            null
        } else {
            roads[1]
        }
    }

    private fun checkDevopsPermission(request: CheckPermissionRequest): Boolean {
        with(request) {
            logger.info(
                "checkDevopsPermission, platformAppId: $appId, userId: $uid, projectId: $projectId, " +
                    "repoName: $repoName, path: $path, action: $action"
            )
            // 网关请求不允许匿名访问
            if (appId == bkAuthConfig.bkrepoAppId && request.uid == ANONYMOUS_USER) {
                if (request.uid == ANONYMOUS_USER) {
                    logger.warn("no anonymous access")
                    return false
                }
            }

            if (!bkAuthConfig.devopsAuthEnabled) return true // devops 鉴权未开启
            if (request.uid == ANONYMOUS_USER && bkAuthConfig.devopsAllowAnonymous) return true // 允许 devops 匿名访问

            when (repoName) {
                CUSTOM -> {
                    return checkProjectPermission(uid, projectId!!)
                }
                PIPELINE -> {
                    return when (resourceType) {
                        ResourceType.REPO -> checkProjectPermission(uid, projectId!!)
                        ResourceType.NODE -> {
                            val pipelineId = parsePipelineId(path!!) ?: return false
                            checkPipelinePermission(uid, projectId!!, pipelineId)
                        }
                        else -> throw RuntimeException("resource type not supported: $resourceType")
                    }
                }
                REPORT -> {
                    return action == PermissionAction.READ || action == PermissionAction.WRITE
                }
                else -> {
                    logger.warn("invalid repoName: $repoName")
                    return false
                }
            }
        }
    }

    private fun checkPipelinePermission(uid: String, projectId: String, pipelineId: String): Boolean {
        logger.info("checkPipelinePermission, uid: $uid, projectId: $projectId, pipelineId: $pipelineId")
        return try {
            return bkAuthService.validateUserResourcePermission(
                user = uid,
                serviceCode = BkAuthServiceCode.PIPELINE,
                resourceType = BkAuthResourceType.PIPELINE_DEFAULT,
                projectCode = projectId,
                resourceCode = pipelineId,
                permission = BkAuthPermission.DOWNLOAD,
                retryIfTokenInvalid = true
            )
        } catch (e: Exception) {
            // TODO 调用auth稳定后改为抛异常
            logger.warn("checkPipelinePermission error:  ${e.message}")
            true
        }
    }

    private fun checkProjectPermission(uid: String, projectId: String): Boolean {
        logger.info("checkProjectPermission: uid: $uid, projectId: $projectId")
        return try {
            bkAuthProjectService.isProjectMember(uid, projectId, retryIfTokenInvalid = true)
        } catch (e: Exception) {
            // TODO 调用auth稳定后改为抛异常
            logger.warn("checkPipelinePermission error:  ${e.message}")
            true
        }
    }

    private fun isDevopsRepo(repoName: String): Boolean {
        return repoName == CUSTOM || repoName == PIPELINE || repoName == REPORT
    }

    override fun checkPermission(request: CheckPermissionRequest): Boolean {
        logger.info("check permission, request : $request")

        // 校验蓝盾平台账号项目权限
        if (request.resourceType == ResourceType.PROJECT && request.appId == bkAuthConfig.devopsAppId) {
            return true
        }

        // 校验蓝盾/网关平台账号指定仓库(pipeline/custom/report)的仓库和节点权限
        if ((request.resourceType == ResourceType.REPO || request.resourceType == ResourceType.NODE) &&
            isDevopsRepo(request.repoName!!) &&
            (request.appId == bkAuthConfig.devopsAppId || request.appId == bkAuthConfig.bkrepoAppId)
        ) {
            return checkDevopsPermission(request)
        }

        return super.checkPermission(request)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkAuthPermissionServiceImpl::class.java)
        private const val CUSTOM = "custom"
        private const val PIPELINE = "pipeline"
        private const val REPORT = "report"
    }
}
