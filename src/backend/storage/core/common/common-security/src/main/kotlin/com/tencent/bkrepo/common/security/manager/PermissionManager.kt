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

package com.tencent.bkrepo.common.security.manager

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.tencent.bkrepo.auth.api.ServiceExternalPermissionResource
import com.tencent.bkrepo.auth.api.ServicePermissionResource
import com.tencent.bkrepo.auth.api.ServiceUserResource
import com.tencent.bkrepo.auth.pojo.RegisterResourceRequest
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.externalPermission.ExternalPermission
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.constant.PIPELINE
import com.tencent.bkrepo.common.artifact.exception.NodeNotFoundException
import com.tencent.bkrepo.common.artifact.exception.RepoNotFoundException
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.security.exception.AuthenticationException
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.security.http.core.HttpAuthProperties
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.constant.NODE_DETAIL_LIST_KEY
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 权限管理类
 */
open class PermissionManager(
    private val repositoryClient: RepositoryClient,
    private val permissionResource: ServicePermissionResource,
    private val externalPermissionResource: ServiceExternalPermissionResource,
    private val userResource: ServiceUserResource,
    private val httpAuthProperties: HttpAuthProperties,
    private val nodeClient: NodeClient
) {

    private val httpClient =
        OkHttpClient.Builder().connectTimeout(10L, TimeUnit.SECONDS).readTimeout(10L, TimeUnit.SECONDS).build()

    private val externalPermissionCache: LoadingCache<String, List<ExternalPermission>> by lazy {
        val cacheLoader = object : CacheLoader<String, List<ExternalPermission>>() {
            override fun load(key: String): List<ExternalPermission> =
                externalPermissionResource.listExternalPermission().data!!
        }
        CacheBuilder.newBuilder().maximumSize(1).expireAfterWrite(30L, TimeUnit.MINUTES).build(cacheLoader)
    }

    /**
     * 校验项目权限
     * @param action 动作
     * @param projectId 项目id
     */
    open fun checkProjectPermission(
        action: PermissionAction, projectId: String
    ) {
        checkPermission(ResourceType.PROJECT, action, projectId)
    }

    /**
     * 校验仓库权限
     * @param action 动作
     * @param projectId 项目id
     * @param repoName 仓库名称
     * @param public 仓库是否为public
     * @param anonymous 是否允许匿名
     */
    open fun checkRepoPermission(
        action: PermissionAction,
        projectId: String,
        repoName: String,
        public: Boolean? = null,
        anonymous: Boolean = false
    ) {
        val repoInfo = queryRepositoryInfo(projectId, repoName)
        if (isReadPublicRepo(action, repoInfo, public)) {
            return
        }
        if (isReadSystemRepo(action, repoInfo)) {
            return
        }
        checkPermission(
            type = ResourceType.REPO,
            action = action,
            projectId = projectId,
            repoName = repoName,
            anonymous = anonymous
        )
    }

    /**
     * 校验节点权限
     * @param action 动作
     * @param projectId 项目id
     * @param repoName 仓库名称
     * @param path 节点路径
     * @param public 仓库是否为public
     * @param anonymous 是否允许匿名
     */
    open fun checkNodePermission(
        action: PermissionAction,
        projectId: String,
        repoName: String,
        vararg path: String,
        public: Boolean? = null,
        anonymous: Boolean = false
    ) {
        val repoInfo = queryRepositoryInfo(projectId, repoName)
        if (isReadPublicRepo(action, repoInfo, public)) {
            return
        }
        if (isReadSystemRepo(action, repoInfo)) {
            return
        }
        // 禁止批量下载流水线节点
        if (path.size > 1 && repoName == PIPELINE) {
            throw PermissionException()
        }

        checkPermission(
            type = ResourceType.NODE,
            action = action,
            projectId = projectId,
            repoName = repoName,
            paths = path.toList(),
            anonymous = anonymous
        )
    }

    /**
     * 校验身份
     * @param userId 用户id
     * @param principalType 身份类型
     */
    fun checkPrincipal(userId: String, principalType: PrincipalType) {
        if (!httpAuthProperties.enabled) {
            return
        }
        val platformId = SecurityUtils.getPlatformId()
        checkAnonymous(userId, platformId)

        if (principalType == PrincipalType.ADMIN) {
            if (!isAdminUser(userId)) {
                throw PermissionException()
            }
        } else if (principalType == PrincipalType.PLATFORM) {
            if (platformId == null && !isAdminUser(userId)) {
                throw PermissionException()
            }
        }
    }

    fun registerProject(userId: String, projectId: String) {
        val request = RegisterResourceRequest(userId, ResourceType.PROJECT.toString(), projectId)
        permissionResource.registerResource(request)
    }

    fun registerRepo(userId: String, projectId: String, repoName: String) {
        val request = RegisterResourceRequest(userId, ResourceType.REPO.toString(), projectId, repoName)
        permissionResource.registerResource(request)
    }

    /**
     * 判断是否为public仓库且为READ操作
     */
    private fun isReadPublicRepo(
        action: PermissionAction, repoInfo: RepositoryInfo, public: Boolean? = null
    ): Boolean {
        if (action != PermissionAction.READ) {
            return false
        }
        return public ?: repoInfo.public
    }

    /**
     * 判断是否为系统级公开仓库且为READ操作
     */
    @Suppress("TooGenericExceptionCaught")
    private fun isReadSystemRepo(action: PermissionAction, repoInfo: RepositoryInfo): Boolean {
        if (action != PermissionAction.READ) {
            return false
        }
        val userId = SecurityUtils.getUserId()
        val platformId = SecurityUtils.getPlatformId()
        checkAnonymous(userId, platformId)
        // 加载仓库信息
        val systemValue = repoInfo.configuration.settings["system"]
        val system = try {
            systemValue as? Boolean
        } catch (e: Exception) {
            logger.error("Repo configuration system field trans failed: $systemValue", e)
            false
        }
        return true == system
    }

    /**
     * 查询仓库信息
     */
    private fun queryRepositoryInfo(projectId: String, repoName: String): RepositoryInfo {
        return repositoryClient.getRepoInfo(projectId, repoName).data ?: throw RepoNotFoundException(repoName)
    }

    /**
     * 去auth微服务校验资源权限
     */
    private fun checkPermission(
        type: ResourceType,
        action: PermissionAction,
        projectId: String,
        repoName: String? = null,
        paths: List<String>? = null,
        anonymous: Boolean = false
    ) {
        // 判断是否开启认证
        if (!httpAuthProperties.enabled) {
            return
        }
        val userId = SecurityUtils.getUserId()
        val platformId = SecurityUtils.getPlatformId()
        checkAnonymous(userId, platformId)

        if (userId == ANONYMOUS_USER && platformId != null && anonymous) {
            return
        }

        // 校验Oauth token对应权限
        val authorities = SecurityUtils.getAuthorities()
        if (authorities.isNotEmpty() && !authorities.contains(type.toString())) {
            throw PermissionException()
        }

        // 自定义外部权限校验
        val externalPermission = getExternalPermission(projectId, repoName)
        if (externalPermission != null) {
            checkExternalPermission(externalPermission, userId, type, action, projectId, repoName, paths)
            return
        }

        // 去auth微服务校验资源权限
        val checkRequest = CheckPermissionRequest(
            uid = userId,
            appId = platformId,
            resourceType = type.toString(),
            action = action.toString(),
            projectId = projectId,
            repoName = repoName,
            path = paths?.first()
        )
        if (permissionResource.checkPermission(checkRequest).data != true) {
            // 无权限，响应403错误
            var reason = "user[$userId] does not have $action permission in project[$projectId]"
            repoName?.let { reason += " repo[$repoName]" }
            throw PermissionException(reason)
        }
        if (logger.isDebugEnabled) {
            logger.debug("User[${SecurityUtils.getPrincipal()}] check permission success.")
        }
    }

    /**
     * 获取当前项目、仓库的自定义外部权限
     */
    private fun getExternalPermission(projectId: String, repoName: String?): ExternalPermission? {
        val externalPermissionList = externalPermissionCache.get(SYSTEM_USER)
        val platformId = SecurityUtils.getPlatformId()
        val ext = externalPermissionList.firstOrNull { p ->
            p.enabled.and(projectId.matches(wildcardToRegex(p.projectId)))
                .and(repoName?.matches(wildcardToRegex(p.repoName)) ?: true).and(matchApi(p.scope))
                .and(p.platformWhiteList.isNullOrEmpty() || !p.platformWhiteList!!.contains(platformId))
        }
        return ext
    }

    /**
     * 匹配需要自定义鉴权的接口
     * 通过straceTrace获取接口名称
     *   1. 过滤包名为com.tencent.bkrepo的接口
     *   2. 使用注解鉴权的接口是由Spring cglib生成的，类名中包含$$EnhancerBySpringCGLIB$$xxxx, 需要替换掉
     *      例如com.tencent.bkrepo.generic.controller.GenericController$$EnhancerBySpringCGLIB$$bccb61f5.download()
     *   3. 去掉括号，得到接口名称
     *      例如com.tencent.bkrepo.generic.controller.GenericController.download
     * 然后scope与接口名称匹配进行正则匹配
     */
    private fun matchApi(scope: String): Boolean {
        val stackTraceElements =
            Thread.currentThread().stackTrace.toList().filter { it.toString().startsWith(PACKAGE_NAME_PREFIX) }.map {
                it.toString().replace(Regex("\\\$\\\$(.*)\\\$\\\$[a-z0-9]+"), "")
                    .substringBefore("(")
            }
        logger.debug("stack trace elements: $stackTraceElements")
        val pattern = wildcardToRegex(scope)
        stackTraceElements.forEach {
            if (pattern.matches(it)) {
                logger.debug("scope[$scope] match api: $it")
                return true
            }
        }
        return false
    }

    /**
     * 检查外部权限
     */
    private fun checkExternalPermission(
        externalPermission: ExternalPermission,
        userId: String,
        type: ResourceType,
        action: PermissionAction,
        projectId: String,
        repoName: String?,
        paths: List<String>?
    ) {
        var errorMsg = "user[$userId] does not have $action permission in project[$projectId] repo[$repoName]"
        paths?.let { errorMsg = errorMsg.plus(" path$paths") }

        val nodes = getNodeDetailList(projectId, repoName, paths)

        val request = buildRequest(externalPermission, type, action, userId, projectId, repoName, nodes)
        callbackToAuth(request, projectId, repoName, paths, errorMsg)
    }

    private fun getNodeDetailList(
        projectId: String, repoName: String?, paths: List<String>?
    ): List<NodeDetail>? {
        val nodeDetailList = if (repoName.isNullOrBlank() || paths.isNullOrEmpty()) {
            null
        } else if (paths.size == 1) {
            val node = nodeClient.getNodeDetail(projectId, repoName, paths.first()).data ?: throw NodeNotFoundException(
                paths.first()
            )
            listOf(node)
        } else {
            queryNodeDetailList(projectId, repoName, paths)
        }
        if (!nodeDetailList.isNullOrEmpty()) {
            HttpContextHolder.getRequest().setAttribute(NODE_DETAIL_LIST_KEY, nodeDetailList)
        }
        return nodeDetailList
    }

    private fun queryNodeDetailList(
        projectId: String, repoName: String, paths: List<String>
    ): List<NodeDetail> {
        var prefix = paths.first()
        paths.forEach {
            prefix = PathUtils.getCommonPath(prefix, it)
        }
        var pageNumber = 1
        val nodeDetailList = mutableListOf<NodeDetail>()
        do {
            val option = NodeListOption(
                pageNumber = pageNumber, pageSize = 1000, includeFolder = true, includeMetadata = true, deep = true
            )
            val records = nodeClient.listNodePage(projectId, repoName, prefix, option).data?.records
            if (records.isNullOrEmpty()) {
                break
            }
            nodeDetailList.addAll(records.filter { paths.contains(it.fullPath) }.map { NodeDetail(it) })
            pageNumber++
        } while (nodeDetailList.size < paths.size)
        return nodeDetailList
    }


    private fun callbackToAuth(
        request: Request, projectId: String, repoName: String?, paths: List<String>?, errorMsg: String
    ) {
        try {
            httpClient.newCall(request).execute().use {
                val content = it.body()?.string()
                if (it.isSuccessful && checkResponse(content)) {
                    return
                }
                logger.info(
                    "check external permission error, url[${request.url()}], project[$projectId], repo[$repoName]," +
                            " nodes$paths, code[${it.code()}], response[$content]"
                )
                throw PermissionException(errorMsg)
            }

        } catch (e: IOException) {
            logger.error(
                "check external permission error," + "url[${request.url()}], project[$projectId], " +
                        "repo[$repoName], nodes$paths, $e"
            )
            throw PermissionException(errorMsg)
        }
    }

    private fun checkResponse(content: String?): Boolean {
        if (content.isNullOrBlank()) {
            return true
        }
        logger.debug("response content: $content")
        val data = content.readJsonString<Response<*>>()
        if (data.isNotOk()) {
            return false
        }
        return true
    }

    private fun buildRequest(
        externalPermission: ExternalPermission,
        type: ResourceType,
        action: PermissionAction,
        userId: String,
        projectId: String,
        repoName: String?,
        nodes: List<NodeDetail>?
    ): Request {
        val headersBuilder = Headers.Builder()
        externalPermission.headers?.forEach { (k, v) ->
            headersBuilder[k] = v
        }
        val requestData = mutableMapOf<String, Any>()
        requestData[USER_ID] = userId
        requestData[TYPE] = type.toString()
        requestData[ACTION] = action.toString()
        requestData[PROJECT_ID] = projectId
        repoName?.let { requestData[REPO_NAME] = repoName }
        nodes?.let {
            val nodeMaps = mutableListOf<Map<String, Any>>()
            it.forEach { nodeDetail ->
                nodeMaps.add(
                    mapOf(
                        FULL_PATH to nodeDetail.fullPath, METADATA to nodeDetail.metadata
                    )
                )
            }
            requestData[NODES] = nodeMaps
        }
        val requestBody = RequestBody.create(MediaType.parse(MediaTypes.APPLICATION_JSON), requestData.toJsonString())
        logger.debug("request data: ${requestData.toJsonString()}")
        return Request.Builder().url(externalPermission.url).headers(headersBuilder.build()).post(requestBody).build()
    }

    /**
     * 判断是否为管理员
     */
    private fun isAdminUser(userId: String): Boolean {
        return userResource.detail(userId).data?.admin == true
    }

    fun enableAuth(): Boolean {
        return httpAuthProperties.enabled
    }

    companion object {

        private val logger = LoggerFactory.getLogger(PermissionManager::class.java)
        private val keywordList = listOf("\\", "$", "(", ")", "+", ".", "[", "]", "?", "^", "{", "}", "|", "?", "&")

        private const val USER_ID = "userId"
        private const val TYPE = "type"
        private const val ACTION = "action"
        private const val PROJECT_ID = "projectId"
        private const val REPO_NAME = "repoName"
        private const val FULL_PATH = "fullPath"
        private const val METADATA = "metadata"
        private const val NODES = "nodes"
        private const val PACKAGE_NAME_PREFIX = "com.tencent.bkrepo"

        /**
         * 检查是否为匿名用户，如果是匿名用户则返回401并提示登录
         */
        private fun checkAnonymous(userId: String, platformId: String?) {
            if (userId == ANONYMOUS_USER && platformId == null) {
                throw AuthenticationException()
            }
        }

        private fun wildcardToRegex(input: String): Regex {
            var escapedString = input.trim()
            if (escapedString.isNotBlank()) {
                keywordList.forEach {
                    if (escapedString.contains(it)) {
                        escapedString = escapedString.replace(it, "\\$it")
                    }
                }
            }
            return Regex(escapedString.replace("*", ".*"))

        }
    }
}
