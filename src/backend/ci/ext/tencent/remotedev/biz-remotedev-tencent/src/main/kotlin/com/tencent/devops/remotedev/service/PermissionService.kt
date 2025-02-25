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

package com.tencent.devops.remotedev.service

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.auth.api.oauth2.Oauth2ServiceEndpointResource
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.auth.pojo.Oauth2PassWordRequest
import com.tencent.devops.auth.pojo.enum.Oauth2GrantType
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.common.api.constant.HTTP_401
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OauthForbiddenException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.ServiceUserResource
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.pojo.CdsToken
import com.tencent.devops.remotedev.pojo.UserOnePassword
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.service.redis.ConfigCacheService
import java.net.URLEncoder
import java.util.Base64
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class PermissionService @Autowired constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext,
    private val workspaceDao: WorkspaceDao,
    private val workspaceSharedDao: WorkspaceSharedDao,
    private val redisCache: ConfigCacheService,
    private val checkTokenService: ClientTokenService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PermissionService::class.java)
        private const val REDIS_KEY = "remotedev_1Password:"
        private const val EXPIRED_SECOND = 5L
    }

    @Value("\${remoteDev.enablePermission:true}")
    private val enablePermission: Boolean = true

    @Value("\${remoteDev.aes.key:}")
    private val aesKey: String = ""

    @Value("\${remoteDev.aes.iv:}")
    private val aesIV: String = ""

    @Value("\${remoteDev.cdi.clientId:}")
    private val clientId: String = ""

    @Value("\${remoteDev.cdi.clientSecret:}")
    private val clientSecret: String = ""

    private val workspaceOwnerCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build(
            object : CacheLoader<String, List<String>>() {
                override fun load(name: String): List<String> {
                    val ws = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = name) ?: return emptyList()
                    return if (ws.ownerType == WorkspaceOwnerType.PERSONAL) {
                        listOf(ws.createUserId)
                    } else {
                        workspaceSharedDao.fetchWorkspaceSharedInfo(
                            dslContext = dslContext,
                            workspaceName = ws.workspaceName,
                            assignType = WorkspaceShared.AssignType.OWNER
                        ).map { it.sharedUser }
                    }
                }
            }
        )

    private val workspaceViewerCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build(
            object : CacheLoader<String, List<String>>() {
                override fun load(name: String): List<String> {
                    return workspaceDao.fetchWorkspaceUser(dslContext, name)
                }
            }
        )

    private val projectManagerCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build(
            object : CacheLoader<String, List<String>>() {
                override fun load(projectId: String): List<String> {
                    return managers(projectId).ifEmpty { managersOld(projectId) }
                }
            }
        )

    fun getWorkspaceOwner(workspaceName: String): List<String> = workspaceOwnerCache.get(workspaceName)

    fun checkOwnerPermission(userId: String, workspaceName: String, projectId: String, ownerType: WorkspaceOwnerType) {
        if (!enablePermission) return

        if (!workspaceOwnerCache.get(workspaceName).contains(userId)) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("You need owner permission to access workspace $workspaceName")
            )
        }

        if (ownerType.projectUse() && !checkUserVisitPermission(userId, projectId)) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("You need permission to access project $projectId")
            )
        }
    }

    fun hasOwnerPermission(
        userId: String,
        workspaceName: String,
        projectId: String,
        ownerType: WorkspaceOwnerType
    ): Boolean {
        kotlin.runCatching {
            checkOwnerPermission(
                userId = userId,
                workspaceName = workspaceName,
                projectId = projectId,
                ownerType = ownerType
            )
        }.fold(
            { return true }, {
                logger.warn("not has Owner Permission|$userId, $workspaceName, $projectId")
                return false
            }
        )
    }

    fun hasViewerPermission(userId: String, workspaceName: String, projectId: String): Boolean {
        kotlin.runCatching {
            checkViewerPermission(userId = userId, workspaceName = workspaceName, projectId = projectId)
        }.fold(
            { return true }, {
                logger.warn("not has viewer Permission|$userId, $workspaceName, $projectId")
                return false
            }
        )
    }

    fun checkViewerPermission(userId: String, workspaceName: String, projectId: String) {
        if (!enablePermission) return

        if (!workspaceViewerCache.get(workspaceName).contains(userId)) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("You need viewer permission to access workspace $workspaceName")
            )
        }

        if (!checkUserVisitPermission(userId, projectId) && !redisCache.checkExpertSupportUser(userId)) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("You need permission to access project $projectId")
            )
        }
    }

    fun checkUserProjectManager(userId: String, projectId: String) {
        val checkProjectManager = client.get(ServiceProjectAuthResource::class).checkProjectManager(
            token = checkTokenService.getSystemToken(),
            userId = userId,
            projectCode = projectId
        ).data ?: false
        if (!checkProjectManager) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("You need permission to access project $projectId")
            )
        }
    }

    fun checkUserManager(userId: String, projectId: String) {
        val managers = managers(projectId).ifEmpty { managersOld(projectId) }
        val checkProjectManager = client.get(ServiceProjectAuthResource::class).checkProjectManager(
            token = checkTokenService.getSystemToken(),
            userId = userId,
            projectCode = projectId
        ).data ?: false

        if (!checkProjectManager && userId !in managers) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("You need permission to access project $projectId")
            )
        }
    }

    @Deprecated("use managers instead, 暂时保留")
    fun managersOld(projectId: String): List<String> {
        logger.error("use managersOld instead, 暂时保留")
        val projectInfo = kotlin.runCatching {
            client.get(ServiceProjectResource::class).get(projectId)
        }.onFailure { logger.warn("get project $projectId info error|${it.message}") }
            .getOrElse { null }?.data ?: throw ErrorCodeException(
            errorCode = ProjectMessageCode.PROJECT_NOT_EXIST
        )
        return projectInfo.properties?.remotedevManager?.split(";") ?: emptyList()
    }

    fun managers(projectId: String): List<String> {
        return client.get(ServiceUserResource::class)
            .getProjectUserRoles(projectId, BkAuthGroup.CGS_MANAGER).data ?: emptyList()
    }

    fun getCacheManager(projectId: String): List<String> {
        return projectManagerCache.get(projectId)
    }

    fun hasUserManager(userId: String, projectId: String): Boolean {
        kotlin.runCatching { checkUserManager(userId, projectId) }.fold(
            { return true }, {
                logger.warn("not has manager Permission|$userId, $projectId")
                return false
            }
        )
    }

    fun hasManagerOrOwnerPermission(
        userId: String,
        projectId: String,
        workspaceName: String,
        ownerType: WorkspaceOwnerType
    ): Boolean {
        return hasOwnerPermission(
            userId = userId,
            workspaceName = workspaceName,
            projectId = projectId,
            ownerType = ownerType
        ) || hasUserManager(userId, projectId)
    }

    fun hasManagerOrViewerPermission(userId: String, projectId: String, workspaceName: String): Boolean {
        return hasViewerPermission(
            userId = userId,
            workspaceName = workspaceName,
            projectId = projectId
        ) || hasUserManager(userId, projectId)
    }

    // 判断用户是否项目成员
    fun checkUserVisitPermission(
        userId: String,
        projectCode: String
    ): Boolean {
        return kotlin.runCatching {
            client.get(ServiceProjectAuthResource::class).checkUserInProjectLevelGroup(
                token = checkTokenService.getSystemToken(),
                projectCode = projectCode,
                userId = userId
            ).data
        }.getOrNull() ?: false
    }

    private fun initRedisUser(params: UserOnePassword, expiredInSecond: Long?): String {
        val key = Base64.getEncoder().encodeToString(UUIDUtil.generate().toByteArray())
        redisOperation.set(
            key = REDIS_KEY + key,
            value = JsonUtil.toJson(params, false),
            expiredInSecond = expiredInSecond ?: EXPIRED_SECOND
        )
        return key
    }

    fun checkAndGetUser1Password(key: String): UserOnePassword {
        val value = redisOperation.get(REDIS_KEY + key)
        if (value.isNullOrBlank()) {
            throw OperationException("Session is already registered or has expired.Please reapply for authorization.")
        }
        redisOperation.delete(REDIS_KEY + key)
        return JsonUtil.to(value, object : TypeReference<UserOnePassword>() {})
    }

    fun checkAndGetUser1PasswordNoDelete(key: String): UserOnePassword {
        val value = redisOperation.get(REDIS_KEY + key)
        if (value.isNullOrBlank()) {
            throw OperationException("Session is already registered or has expired.Please reapply for authorization.")
        }
        return JsonUtil.to(value, object : TypeReference<UserOnePassword>() {})
    }

    fun init1Password(userId: String, workspaceName: String, projectId: String?, expiredInSecond: Long?): String {
        val key = initRedisUser(
            UserOnePassword(
                userId, workspaceName, projectId
            ),
            expiredInSecond
        )
        logger.info("start init1Password|$userId|$workspaceName|$key")
        return URLEncoder.encode(key, "UTF-8")
    }

    fun checkCdsToken(cdsToken: String): CdsToken {
        return kotlin.runCatching {
            val value = AESUtil.decryptWithIV(aesKey, aesIV, cdsToken)
            val split = value.split("::::")
            CdsToken(split[0], split[1], split[2], split[3])
        }.onFailure {
            logger.warn("checkCdsToken error|$cdsToken|${it.message}")
            throw OperationException("cdsToken is illegal.")
        }.getOrThrow()
    }

    fun checkUserPermission(userId: String, workspaceName: String): Boolean {
        if (!enablePermission) return true

        if (!workspaceViewerCache.get(workspaceName).contains(userId)) {
            return false
        }
        return true
    }

    fun getCDIOauth(workspaceName: String, appId: String): Oauth2AccessTokenVo {
        val key = "$workspaceName@@$appId"
        return client.get(Oauth2ServiceEndpointResource::class).getAccessToken(
            clientId,
            clientSecret,
            Oauth2PassWordRequest(
                Oauth2GrantType.PASS_WORD,
                userName = key,
                passWord = key
            )
        ).data!!
    }

    fun checkCDIOauth(cdiToken: String): Pair<String, String>? {
        kotlin.runCatching {
            checkNotNull(
                client.get(Oauth2ServiceEndpointResource::class).verifyAccessToken(
                    clientId = clientId,
                    clientSecret = clientSecret,
                    accessToken = cdiToken
                ).data
            ).split("@@")
        }.fold(
            {
                return it[0] to it[1]
            },
            {
                logger.warn("checkCDIOauth fail|$cdiToken", it)
                return null
            }
        )
    }

    /**
     * 检查工蜂接口是否返回401，针对这种情况，抛出OAUTH_ILLEGAL 让前端跳转去重新授权
     */
    fun <T> checkOauthIllegal(userId: String, action: () -> T): T {
        return kotlin.runCatching {
            action()
        }.onFailure {
            if (it is RemoteServiceException && it.httpStatus == HTTP_401 || it is OauthForbiddenException) {
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.OAUTH_ILLEGAL.errorCode,
                    params = arrayOf(userId)
                )
            }
        }.getOrThrow()
    }
}
