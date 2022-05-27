/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.repository.service.file.impl

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactService
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.repository.dao.TemporaryTokenDao
import com.tencent.bkrepo.repository.model.TTemporaryToken
import com.tencent.bkrepo.repository.pojo.token.TemporaryTokenCreateRequest
import com.tencent.bkrepo.repository.pojo.token.TemporaryTokenInfo
import com.tencent.bkrepo.repository.service.file.TemporaryTokenService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * 临时token服务实现类
 */
@Service
class TemporaryTokenServiceImpl(
    private val temporaryTokenDao: TemporaryTokenDao
) : TemporaryTokenService, ArtifactService() {

    override fun createToken(request: TemporaryTokenCreateRequest): List<TemporaryTokenInfo> {
        with(request) {
            return validateAndNormalize(this).map {
                val temporaryToken = TTemporaryToken(
                    projectId = projectId,
                    repoName = repoName,
                    fullPath = it,
                    expireDate = computeExpireDate(request.expireSeconds),
                    authorizedUserList = request.authorizedUserSet,
                    authorizedIpList = request.authorizedIpSet,
                    token = generateToken(),
                    permits = permits,
                    type = type,
                    createdBy = SecurityUtils.getUserId(),
                    createdDate = LocalDateTime.now(),
                    lastModifiedBy = SecurityUtils.getUserId(),
                    lastModifiedDate = LocalDateTime.now()
                )
                temporaryTokenDao.save(temporaryToken)
                logger.info("Create share record[$temporaryToken] success.")
                convert(temporaryToken)
            }
        }
    }

    override fun getTokenInfo(token: String): TemporaryTokenInfo? {
        val temporaryToken = temporaryTokenDao.findByToken(token) ?: return null
        return convert(temporaryToken)
    }

    override fun deleteToken(token: String) {
        temporaryTokenDao.deleteByToken(token)
        logger.info("Delete temporary token[$token] success.")
    }

    override fun decrementPermits(token: String) {
        temporaryTokenDao.decrementPermits(token)
        logger.info("Decrement permits of token[$token] success.")
    }

    /**
     * 验证数据格式， 格式化fullPath
     */
    private fun validateAndNormalize(request: TemporaryTokenCreateRequest): List<String> {
        with(request) {
            Preconditions.checkArgument(permits == null || permits!! > 0, "permits")
            return fullPathSet.map { PathUtils.normalizeFullPath(it) }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TemporaryTokenServiceImpl::class.java)

        private fun generateToken(): String {
            return UUID.randomUUID().toString().replace(StringPool.DASH, StringPool.EMPTY).toLowerCase()
        }

        private fun computeExpireDate(expireSeconds: Long?): LocalDateTime? {
            return if (expireSeconds == null || expireSeconds <= 0) null
            else LocalDateTime.now().plusSeconds(expireSeconds)
        }

        private fun convert(tTemporaryToken: TTemporaryToken): TemporaryTokenInfo {
            return tTemporaryToken.let {
                TemporaryTokenInfo(
                    fullPath = it.fullPath,
                    repoName = it.repoName,
                    projectId = it.projectId,
                    token = it.token,
                    authorizedUserList = it.authorizedUserList,
                    authorizedIpList = it.authorizedIpList,
                    expireDate = it.expireDate?.format(DateTimeFormatter.ISO_DATE_TIME),
                    type = it.type,
                    permits = it.permits,
                    createdBy = it.createdBy
                )
            }
        }
    }
}
