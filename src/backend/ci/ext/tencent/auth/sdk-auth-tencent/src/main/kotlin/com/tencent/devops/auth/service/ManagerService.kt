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

package com.tencent.devops.auth.service

import com.google.common.cache.CacheBuilder
import com.tencent.devops.auth.api.manager.ServiceManagerUserResource
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_USER_CONTRACT_NOT_SIGNED
import com.tencent.devops.auth.pojo.ProjectOrgInfo
import com.tencent.devops.auth.pojo.UserPermissionInfo
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.service.ServiceSignatureManageResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.util.concurrent.TimeUnit

class ManagerService @Autowired constructor(
    val client: Client
) {

    @Autowired
    private lateinit var redisOperation: RedisOperation

    @Autowired
    private lateinit var config: CommonConfig

    @Value("\${auth.eSignature.verificationControl:true}")
    private var eSignatureVerificationControl: Boolean = true

    private val userPermissionMap = CacheBuilder.newBuilder()
        .maximumSize(50000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String/*userId*/, Map<String/*organizationId*/, UserPermissionInfo>>()

    private val projectInfoMap = CacheBuilder.newBuilder()
        .maximumSize(50000)
        .expireAfterWrite(60, TimeUnit.MINUTES)
        .build<String/*userId*/, ProjectOrgInfo?>()

    private val user2ESignStatus = CacheBuilder.newBuilder()
        .maximumSize(50000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String/*platform:userId*/, Boolean>()

    // 需要签名验证的项目本地缓存
    private val projectsRequiringSignatureVerificationCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String/*projectId*/, Boolean>()

    // 需要签名预检查的项目本地缓存
    private val projectsRequiringSignaturePreCheckCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String/*projectId*/, Boolean>()

    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "ComplexMethod")
    fun isManagerPermission(
        userId: String,
        projectId: String,
        resourceType: AuthResourceType,
        authPermission: AuthPermission
    ): Boolean {
        logger.info("isManagerPermission $userId| $projectId| ${resourceType.value} | ${authPermission.value}")
        // 需要签订保密协议的项目，不允许超管和reporter直接查看，需要走正常权限校验逻辑
        if (needESignVerification(projectId)) {
            logger.info("This project requires a contract to be signed before visit. $userId|$projectId")
            return false
        }
        // 新老版本兼容，旧版本项目访问权限为project_view，新版本为project_visit,兼容
        val fixAuthPermission = if (resourceType == AuthResourceType.PROJECT &&
            authPermission == AuthPermission.VISIT) {
            AuthPermission.VIEW
        } else {
            authPermission
        }
        // 从缓存内获取用户管理员信息，若缓存击穿，调用auth服务获取源数据，并刷入内存
        val manageInfo = if (userPermissionMap.getIfPresent(userId) == null) {
            val remoteManagerInfo = client.get(ServiceManagerUserResource::class).getManagerInfo(userId)
            if (remoteManagerInfo.data != null && remoteManagerInfo.data!!.isNotEmpty()) {
                userPermissionMap.put(userId, remoteManagerInfo.data!!)
                remoteManagerInfo.data
            } else {
                null
            }
        } else {
            userPermissionMap.getIfPresent(userId)
        }
        logger.info("user managerInfo $userId| $manageInfo")
        if (manageInfo == null) {
            // 用户没有管理员相关信息
            return false
        }

        // 从缓存内获取项目组织信息，若缓存击穿，调用project服务获取源数据，并刷入内存
        val projectCacheOrgInfo = projectInfoMap.getIfPresent(projectId)

        val projectOrgInfo = if (projectCacheOrgInfo == null) {
            val projectVo = client.get(ServiceProjectResource::class).get(projectId)
            if (projectVo.data == null) {
                logger.info("get projectInfo is empty, $projectId")
                null
            } else {
                val remoteProjectOrgInfo = ProjectOrgInfo(
                    bgId = projectVo.data?.bgId ?: "0",
                    deptId = projectVo.data?.deptId,
                    centerId = projectVo.data?.centerId
                )
                projectInfoMap.put(projectId, remoteProjectOrgInfo)
                remoteProjectOrgInfo
            }
        } else {
            projectInfoMap.getIfPresent(projectId)
        }

        logger.info("project org Info: $projectId, $projectOrgInfo")
        if (projectOrgInfo == null) {
            logger.info("project OrgInfo is empty $projectId")
            return false
        }

        var isManagerPermission = false

        run managerPermissionFor@{
            // 匹配管理员组织信息与项目组织信息
            manageInfo.keys.forEach orgForEach@{ orgId ->
                val managerPermission = manageInfo[orgId] ?: return@orgForEach
                val isOrgEqual =
                    when (managerPermission.organizationLevel) {
                        1 -> projectOrgInfo.bgId == managerPermission.organizationId.toString()
                        2 -> projectOrgInfo.deptId == managerPermission.organizationId.toString()
                        3 -> projectOrgInfo.centerId == managerPermission.organizationId.toString()
                        else -> false
                    }
                if (!isOrgEqual) {
                    // 组织信息未匹配
                    return@orgForEach
                }
                logger.info("managerUser project org check success $userId $projectId $projectOrgInfo")
                // 匹配管理员内的资源类型与用户操作的资源类型
                val orgManagerPermissionMap = managerPermission.permissionMap
                orgManagerPermissionMap.keys.forEach resourceForEach@{ resourceKey ->
                    if (resourceKey == resourceType) {
                        // 资源类型一致的情况下，匹配action是否一致
                        val orgManagerPermissionList = orgManagerPermissionMap[resourceKey]
                        if (orgManagerPermissionList.isNullOrEmpty()) {
                            return@resourceForEach
                        }

                        if (orgManagerPermissionList.contains(fixAuthPermission)) {
                            logger.info(
                                "$userId has $projectId ${resourceType.value} ${fixAuthPermission.value} " +
                                    "$projectOrgInfo manager permission"
                            )
                            isManagerPermission = true
                            return@managerPermissionFor
                        }
                    }
                }
            }
        }
        return isManagerPermission
    }

    fun checkUserESignStatus(
        projectId: String,
        userId: String
    ) {
        if (needESignPreCheck(projectId)) {
            if (!isUserSigned(projectId, userId)) {
                logger.warn(
                    "Pre-process | The user cannot access the project " +
                        "because the contract has not been signed.$projectId|$userId"
                )
            } else {
                logger.info("Pre-process | The user has signed the contract.$projectId|$userId")
            }
            return
        }
        if (needESignVerification(projectId)) {
            if (!isUserSigned(projectId, userId)) {
                logger.warn(
                    "The user cannot access the project because the contract has not been signed.$projectId|$userId"
                )
                throw ErrorCodeException(
                    errorCode = ERROR_USER_CONTRACT_NOT_SIGNED,
                    params = arrayOf(userId, "${config.devopsHostGateway}/console/pipeline/$projectId")
                )
            }
        }
    }

    private fun needESignVerification(projectId: String): Boolean {
        if (!eSignatureVerificationControl) {
            return false
        }
        return isProjectRequiringSignatureVerification(projectId)
    }

    private fun needESignPreCheck(projectId: String): Boolean {
        return isProjectRequiringSignaturePreCheck(projectId)
    }

    private fun isProjectRequiringSignatureVerification(projectId: String): Boolean {
        val cachedValue = projectsRequiringSignatureVerificationCache.getIfPresent(projectId)
        if (cachedValue != null) {
            return cachedValue
        }
        val isMember = redisOperation.isMember(PROJECTS_REQUIRING_SIGNATURE_VERIFICATION, projectId)
        projectsRequiringSignatureVerificationCache.put(projectId, isMember)
        return isMember
    }

    private fun isProjectRequiringSignaturePreCheck(projectId: String): Boolean {
        val cachedValue = projectsRequiringSignaturePreCheckCache.getIfPresent(projectId)
        if (cachedValue != null) {
            return cachedValue
        }
        val isMember = redisOperation.isMember(PROJECTS_REQUIRING_SIGNATURE_PRE_CHECK, projectId)
        projectsRequiringSignaturePreCheckCache.put(projectId, isMember)
        return isMember
    }

    private fun isUserSigned(
        projectId: String,
        userId: String
    ): Boolean {
        val platform = getPlatformByProjectId(projectId) ?: return true
        val localCacheKey = "$platform:$userId"
        //  优先查询本地缓存
        val localCacheValue = user2ESignStatus.getIfPresent(localCacheKey)
        if (localCacheValue != null) {
            return localCacheValue
        }
        // 2. 本地缓存未命中，查询Redis
        val redisValue = isUserSignedInRedisCache(platform, userId)
        if (redisValue) {
            user2ESignStatus.put(localCacheKey, true) // 回填本地缓存
            return true
        }

        // 3. Redis未命中，调用第三方接口
        return runCatching {
            client.get(ServiceSignatureManageResource::class)
                .fetchLiveSignatureStatus(
                    projectId = projectId,
                    userId = userId
                ).data?.signed ?: false
        }.onSuccess { signed ->
            user2ESignStatus.put(localCacheKey, signed)
        }.onFailure { e ->
            logger.error("查询用户[$userId]签署状态失败: ${e.message}", e)
            user2ESignStatus.put(localCacheKey, false) // 降级：异常时缓存false防穿透
        }.getOrDefault(false)
    }

    private fun isUserSignedInRedisCache(platform: String, userId: String): Boolean {
        return redisOperation.isMember(USER_SIGNATURE_STATUS_CACHE_KEY.format(platform), userId)
    }

    private fun getPlatformByProjectId(projectId: String): String? {
        return redisOperation.get(PROJECT_SIGNATURE_PLATFORM_KEY.format(projectId)).also {
            if (it == null) {
                logger.error("get platform by project id failed $projectId")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ManagerService::class.java)
        private const val PROJECTS_REQUIRING_SIGNATURE_VERIFICATION = "projects:signature:verification:required"
        private const val PROJECTS_REQUIRING_SIGNATURE_PRE_CHECK = "projects:signature:pre:check"
        private const val PROJECT_SIGNATURE_PLATFORM_KEY = "projects:signature:%s:platform"
        private const val USER_SIGNATURE_STATUS_CACHE_KEY = "user:signature:status:%s:cache"
    }
}
