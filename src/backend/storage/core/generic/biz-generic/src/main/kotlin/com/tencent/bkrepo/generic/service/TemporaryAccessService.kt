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

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.util.http.UrlFormatter
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.generic.artifact.GenericArtifactInfo
import com.tencent.bkrepo.generic.config.GenericProperties
import com.tencent.bkrepo.generic.pojo.TemporaryAccessToken
import com.tencent.bkrepo.generic.pojo.TemporaryAccessUrl
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.api.TemporaryTokenClient
import com.tencent.bkrepo.repository.pojo.token.TemporaryTokenCreateRequest
import com.tencent.bkrepo.repository.pojo.token.TemporaryTokenInfo
import com.tencent.bkrepo.repository.pojo.token.TokenType
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 临时访问服务
 */
@Service
class TemporaryAccessService(
    private val temporaryTokenClient: TemporaryTokenClient,
    private val repositoryClient: RepositoryClient,
    private val genericProperties: GenericProperties
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
     */
    fun createUrl(request: TemporaryTokenCreateRequest): Response<List<TemporaryAccessUrl>> {
        Preconditions.checkArgument(request.type == TokenType.DOWNLOAD, "type")
        Preconditions.checkArgument(request.permits == null || request.permits!! > 0, "permits")
        val urlList = temporaryTokenClient.createToken(request).data.orEmpty().map {
            TemporaryAccessUrl(
                projectId = it.projectId,
                repoName = it.repoName,
                fullPath = it.fullPath,
                url = generateAccessUrl(it.projectId, it.repoName, it.fullPath, it.token),
                authorizedUserList = it.authorizedUserList,
                authorizedIpList = it.authorizedIpList,
                expireDate = it.expireDate,
                permits = it.permits,
                type = it.type.name
            )
        }
        return ResponseBuilder.success(urlList)
    }

    /**
     * 根据[request]创建临时访问token, type只能为DOWNLOAD
     */
    fun createToken(request: TemporaryTokenCreateRequest): Response<List<TemporaryAccessToken>> {
        Preconditions.checkArgument(request.permits == null || request.permits!! > 0, "permits")
        val tokenList = temporaryTokenClient.createToken(request).data.orEmpty().map {
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
        return ResponseBuilder.success(tokenList)
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

    private fun generateAccessUrl(projectId: String, repoName: String, fullPath: String, token: String): String {
        return StringBuilder(UrlFormatter.formatHost(genericProperties.host))
            .append(TEMPORARY_DOWNLOAD_ENDPOINT)
            .append("/$projectId/$repoName$fullPath")
            .append("?token=")
            .append(token)
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
    private fun checkAccessResource(temporaryToken: TemporaryTokenInfo, artifactInfo: ArtifactInfo) {
        // 校验项目/仓库
        if (temporaryToken.projectId != artifactInfo.projectId || temporaryToken.repoName != artifactInfo.repoName) {
            throw ErrorCodeException(ArtifactMessageCode.TEMPORARY_TOKEN_INVALID)
        }
        // 校验路径
        if (!PathUtils.isSubPath(artifactInfo.getArtifactFullPath(), temporaryToken.fullPath)) {
            throw ErrorCodeException(ArtifactMessageCode.TEMPORARY_TOKEN_INVALID)
        }
    }

    /**
     * 检查授权用户和ip
     */
    private fun checkAuthorization(temporaryToken: TemporaryTokenInfo) {
        // 检查用户授权
        val userId = SecurityUtils.getUserId()
        if (temporaryToken.authorizedUserList.isNotEmpty() && userId !in temporaryToken.authorizedUserList) {
            throw ErrorCodeException(ArtifactMessageCode.TEMPORARY_TOKEN_INVALID)
        }
        // 校验ip授权
        val clientIp = HttpContextHolder.getClientAddress()
        if (temporaryToken.authorizedIpList.isNotEmpty() && clientIp !in temporaryToken.authorizedIpList) {
            throw ErrorCodeException(ArtifactMessageCode.TEMPORARY_TOKEN_INVALID)
        }
    }

    companion object {
        private const val TEMPORARY_DOWNLOAD_ENDPOINT = "temporary/download"
    }
}
