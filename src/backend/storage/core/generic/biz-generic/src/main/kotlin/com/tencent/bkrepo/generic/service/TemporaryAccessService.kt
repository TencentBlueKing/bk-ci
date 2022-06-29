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

package com.tencent.bkrepo.generic.service

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.constant.REPO_KEY
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.util.http.UrlFormatter
import com.tencent.devops.plugin.api.PluginManager
import com.tencent.devops.plugin.api.applyExtension
import com.tencent.bkrepo.common.security.constant.AUTH_HEADER_UID
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.generic.artifact.GenericArtifactInfo
import com.tencent.bkrepo.generic.config.GenericProperties
import com.tencent.bkrepo.generic.extension.TemporaryUrlNotifyContext
import com.tencent.bkrepo.generic.extension.TemporaryUrlNotifyExtension
import com.tencent.bkrepo.generic.pojo.TemporaryAccessToken
import com.tencent.bkrepo.generic.pojo.TemporaryAccessUrl
import com.tencent.bkrepo.generic.pojo.TemporaryUrlCreateRequest
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.api.TemporaryTokenClient
import com.tencent.bkrepo.repository.pojo.token.TemporaryTokenCreateRequest
import com.tencent.bkrepo.repository.pojo.token.TemporaryTokenInfo
import com.tencent.bkrepo.repository.pojo.token.TokenType
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 临时访问服务
 */
@Service
class TemporaryAccessService(
    private val temporaryTokenClient: TemporaryTokenClient,
    private val repositoryClient: RepositoryClient,
    private val genericProperties: GenericProperties,
    private val pluginManager: PluginManager,
    private val deltaSyncService: DeltaSyncService
) {

    /**
     * 上传
     */
    fun upload(artifactInfo: GenericArtifactInfo, file: ArtifactFile) {
        with(artifactInfo) {
            val repo = repositoryClient.getRepoDetail(projectId, repoName).data
                ?: throw ErrorCodeException(ArtifactMessageCode.REPOSITORY_NOT_FOUND, repoName)
            val context = ArtifactUploadContext(repo, file)
            ArtifactContextHolder.getRepository(repo.category).upload(context)
        }
    }

    /**
     * 下载
     */
    fun download(artifactInfo: GenericArtifactInfo) {
        with(artifactInfo) {
            val repo = repositoryClient.getRepoDetail(projectId, repoName).data
                ?: throw ErrorCodeException(ArtifactMessageCode.REPOSITORY_NOT_FOUND, repoName)
            val context = ArtifactDownloadContext(repo)
            ArtifactContextHolder.getRepository(repo.category).download(context)
        }
    }

    /**
     * 根据[request]创建临时访问url
     * type必须指定具体的类型否则无法确定url
     * 创建出的url格式为$host/generic/temporary/$type/$project/$repo/$path?token=$token
     */
    fun createUrl(request: TemporaryUrlCreateRequest): List<TemporaryAccessUrl> {
        with(request) {
            Preconditions.checkArgument(type == TokenType.UPLOAD || type == TokenType.DOWNLOAD, "type")
            Preconditions.checkArgument(permits ?: Int.MAX_VALUE > 0, "permits")
            val temporaryTokenRequest = TemporaryTokenCreateRequest(
                projectId = projectId,
                repoName = repoName,
                fullPathSet = fullPathSet,
                authorizedUserSet = authorizedUserSet,
                authorizedIpSet = authorizedIpSet,
                expireSeconds = expireSeconds,
                permits = permits,
                type = type
            )
            val urlList = temporaryTokenClient.createToken(temporaryTokenRequest).data.orEmpty().map {
                TemporaryAccessUrl(
                    projectId = it.projectId,
                    repoName = it.repoName,
                    fullPath = it.fullPath,
                    url = generateAccessUrl(it, type, host),
                    authorizedUserList = it.authorizedUserList,
                    authorizedIpList = it.authorizedIpList,
                    expireDate = it.expireDate,
                    permits = it.permits,
                    type = it.type.name
                )
            }
            if (needsNotify) {
                val context = TemporaryUrlNotifyContext(
                    userId = SecurityUtils.getUserId(),
                    urlList = urlList
                )
                pluginManager.applyExtension<TemporaryUrlNotifyExtension> { notify(context) }
            }
            return urlList
        }
    }

    /**
     * 根据[request]创建临时访问token
     */
    fun createToken(request: TemporaryTokenCreateRequest): List<TemporaryAccessToken> {
        with(request) {
            Preconditions.checkArgument(permits ?: Int.MAX_VALUE > 0, "permits")
            return temporaryTokenClient.createToken(this).data.orEmpty().map {
                TemporaryAccessToken(
                    projectId = it.projectId,
                    repoName = it.repoName,
                    fullPath = it.fullPath,
                    token = it.token,
                    authorizedUserList = it.authorizedUserList,
                    authorizedIpList = it.authorizedIpList,
                    expireDate = it.expireDate,
                    permits = it.permits,
                    type = it.type.name
                )
            }
        }
    }

    /**
     * 减少[tokenInfo]访问次数
     * 如果[tokenInfo]访问次数 <= 1，则直接删除
     */
    fun decrementPermits(tokenInfo: TemporaryTokenInfo) {
        if (tokenInfo.permits == null) {
            return
        }
        if (tokenInfo.permits!! <= 1) {
            temporaryTokenClient.deleteToken(tokenInfo.token)
        } else {
            temporaryTokenClient.decrementPermits(tokenInfo.token)
        }
    }

    /**
     * 校验[token]是否有效
     * @param token 待校验token
     * @param artifactInfo 访问构件信息
     * @param type 访问类型
     */
    fun validateToken(
        token: String,
        artifactInfo: ArtifactInfo,
        type: TokenType
    ): TemporaryTokenInfo {
        val temporaryToken = checkToken(token)
        checkExpireTime(temporaryToken.expireDate)
        checkAccessType(temporaryToken.type, type)
        checkAccessResource(temporaryToken, artifactInfo)
        checkAuthorization(temporaryToken)
        checkAccessPermits(temporaryToken.permits)
        return temporaryToken
    }

    /**
     * 增量签名
     * */
    fun sign(artifactInfo: GenericArtifactInfo, md5: String?) {
        with(artifactInfo) {
            val repo = repositoryClient.getRepoDetail(projectId, repoName).data
                ?: throw ErrorCodeException(ArtifactMessageCode.REPOSITORY_NOT_FOUND, repoName)
            val request = HttpContextHolder.getRequest()
            request.setAttribute(REPO_KEY, repo)
            deltaSyncService.downloadSignFile(md5)
        }
    }

    /**
     * 合并
     * */
    fun patch(artifactInfo: GenericArtifactInfo, oldFilePath: String, deltaFile: ArtifactFile): SseEmitter {
        with(artifactInfo) {
            val repo = repositoryClient.getRepoDetail(projectId, repoName).data
                ?: throw ErrorCodeException(ArtifactMessageCode.REPOSITORY_NOT_FOUND, repoName)
            val request = HttpContextHolder.getRequest()
            request.setAttribute(REPO_KEY, repo)
            return deltaSyncService.patch(oldFilePath, deltaFile)
        }
    }

    /**
     * 上传sign file
     * */
    fun uploadSignFile(signFile: ArtifactFile, artifactInfo: GenericArtifactInfo, md5: String) {
        deltaSyncService.uploadSignFile(signFile, artifactInfo, md5)
    }

    /**
     * 根据token生成url
     */
    private fun generateAccessUrl(tokenInfo: TemporaryTokenInfo, tokenType: TokenType, host: String?): String {
        val urlHost = if (!host.isNullOrBlank()) host else genericProperties.domain
        val builder = StringBuilder(UrlFormatter.formatHost(urlHost))
        when (tokenType) {
            TokenType.DOWNLOAD -> builder.append(TEMPORARY_DOWNLOAD_ENDPOINT)
            TokenType.UPLOAD -> builder.append(TEMPORARY_UPLOAD_ENDPOINT)
            else -> builder.append(TEMPORARY_DOWNLOAD_ENDPOINT) // default use download
        }
        return builder.append(StringPool.SLASH)
            .append(tokenInfo.projectId)
            .append(StringPool.SLASH)
            .append(tokenInfo.repoName)
            .append(tokenInfo.fullPath)
            .append("?token=")
            .append(tokenInfo.token)
            .toString()
    }

    /**
     * 检查token并返回token信息
     */
    private fun checkToken(token: String): TemporaryTokenInfo {
        if (token.isBlank()) {
            throw ErrorCodeException(ArtifactMessageCode.TEMPORARY_TOKEN_INVALID)
        }
        return temporaryTokenClient.getTokenInfo(token).data
            ?: throw ErrorCodeException(ArtifactMessageCode.TEMPORARY_TOKEN_INVALID)
    }

    /**
     * 检查token是否过期
     */
    private fun checkExpireTime(expireDateString: String?) {
        expireDateString?.let {
            val expireDate = LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME)
            if (expireDate.isBefore(LocalDateTime.now())) {
                throw ErrorCodeException(ArtifactMessageCode.TEMPORARY_TOKEN_EXPIRED)
            }
        }
    }

    /**
     * 检查访问次数
     */
    private fun checkAccessPermits(permits: Int?) {
        permits?.let {
            if (it <= 0) {
                throw ErrorCodeException(ArtifactMessageCode.TEMPORARY_TOKEN_EXPIRED)
            }
        }
    }

    /**
     * 检查访问类型
     */
    private fun checkAccessType(grantedType: TokenType, accessType: TokenType) {
        if (grantedType != TokenType.ALL && grantedType != accessType) {
            throw ErrorCodeException(ArtifactMessageCode.TEMPORARY_TOKEN_INVALID)
        }
    }

    /**
     * 检查访问资源
     */
    private fun checkAccessResource(tokenInfo: TemporaryTokenInfo, artifactInfo: ArtifactInfo) {
        // 校验项目/仓库
        if (tokenInfo.projectId != artifactInfo.projectId || tokenInfo.repoName != artifactInfo.repoName) {
            throw ErrorCodeException(ArtifactMessageCode.TEMPORARY_TOKEN_INVALID)
        }
        // 校验路径
        if (!PathUtils.isSubPath(artifactInfo.getArtifactFullPath(), tokenInfo.fullPath)) {
            throw ErrorCodeException(ArtifactMessageCode.TEMPORARY_TOKEN_INVALID)
        }
    }

    /**
     * 检查授权用户和ip
     */
    private fun checkAuthorization(tokenInfo: TemporaryTokenInfo) {
        // 检查用户授权
        // 获取经过认证的uid
        val authenticatedUid = SecurityUtils.getUserId()
        // 使用认证uid校验授权
        if (tokenInfo.authorizedUserList.isNotEmpty() && authenticatedUid !in tokenInfo.authorizedUserList) {
            throw ErrorCodeException(ArtifactMessageCode.TEMPORARY_TOKEN_INVALID)
        }
        // 获取需要审计的uid
        val auditedUid = if (SecurityUtils.isAnonymous()) {
            HttpContextHolder.getRequest().getHeader(AUTH_HEADER_UID) ?: tokenInfo.createdBy
        } else authenticatedUid
        // 设置审计uid到session中
        HttpContextHolder.getRequestOrNull()?.setAttribute(USER_KEY, auditedUid)
        // 校验ip授权
        val clientIp = HttpContextHolder.getClientAddress()
        if (tokenInfo.authorizedIpList.isNotEmpty() && clientIp !in tokenInfo.authorizedIpList) {
            throw ErrorCodeException(ArtifactMessageCode.TEMPORARY_TOKEN_INVALID)
        }
    }

    companion object {
        private const val TEMPORARY_DOWNLOAD_ENDPOINT = "/temporary/download"
        private const val TEMPORARY_UPLOAD_ENDPOINT = "/temporary/upload"
    }
}
