/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.project.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.auth.api.AuthPlatformApi
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ConventionDao
import com.tencent.devops.project.pojo.convention.ConventionAgreeResponse
import com.tencent.devops.project.pojo.convention.ConventionAgreementItem
import com.tencent.devops.project.pojo.convention.ConventionBody
import com.tencent.devops.project.pojo.convention.ConventionCurrentResponse
import org.jooq.DSLContext
import org.jooq.exception.DataAccessException
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ConventionService(
    private val conventionDao: ConventionDao,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val authPlatformApi: AuthPlatformApi
) {
    fun current(userId: String, lang: String?): ConventionCurrentResponse {
        requireUser(userId)
        if (conventionDao.existsAgreement(dslContext, userId)) {
            return ConventionCurrentResponse(needAgree = false)
        }
        val content = visibleContent(ConventionSupport.normalizeLang(lang))
        if (content == null) {
            return ConventionCurrentResponse(needAgree = false)
        }
        return ConventionCurrentResponse(needAgree = true, content = content)
    }

    fun agree(userId: String): ConventionAgreeResponse {
        requireUser(userId)
        return withLock {
            if (conventionDao.existsAgreement(dslContext, userId)) {
                return@withLock ConventionAgreeResponse(recorded = true)
            }
            try {
                conventionDao.insertAgreement(dslContext, userId, LocalDateTime.now())
                ConventionAgreeResponse(recorded = true)
            } catch (e: DuplicateKeyException) {
                logger.info("convention already agreed|user=$userId")
                ConventionAgreeResponse(recorded = true)
            } catch (e: DataAccessException) {
                if (!isDuplicate(e)) {
                    throw e
                }
                logger.info("convention already agreed|user=$userId")
                ConventionAgreeResponse(recorded = true)
            }
        }
    }

    fun getBody(userId: String, lang: String?): ConventionBody {
        requirePlatformUser(userId)
        val normalized = ConventionSupport.normalizeLang(lang)
        return ConventionBody(
            lang = normalized,
            content = conventionDao.getBody(dslContext, normalized).orEmpty()
        )
    }

    fun saveBody(userId: String, request: ConventionBody): ConventionBody {
        requirePlatformUser(userId)
        val lang = ConventionSupport.normalizeLang(request.lang)
        if (ConventionSupport.blankContent(request.content)) {
            throw ErrorCodeException(
                errorCode = ProjectMessageCode.CONVENTION_CONTENT_EMPTY,
                defaultMessage = "Convention content cannot be empty"
            )
        }
        if (request.content.length > ConventionSupport.MAX_CONTENT_LENGTH) {
            throw ErrorCodeException(
                errorCode = ProjectMessageCode.CONVENTION_CONTENT_TOO_LONG,
                defaultMessage = "Convention content is too long"
            )
        }
        withLock {
            conventionDao.saveBody(dslContext, lang, request.content, userId)
        }
        return ConventionBody(lang = lang, content = request.content)
    }

    fun listAgreements(
        userId: String,
        filterUser: String?,
        page: Int?,
        pageSize: Int?
    ): Page<ConventionAgreementItem> {
        requirePlatformUser(userId)
        val safePage = (page ?: DEFAULT_PAGE).coerceAtLeast(1)
        val safePageSize = (pageSize ?: DEFAULT_PAGE_SIZE).coerceIn(1, MAX_PAGE_SIZE)
        val keyword = filterUser?.trim()?.takeIf { it.isNotEmpty() }
        val count = conventionDao.countAgreements(dslContext, keyword)
        val records = if (count == 0L) {
            emptyList()
        } else {
            conventionDao.listAgreements(
                dslContext = dslContext,
                userKeyword = keyword,
                offset = (safePage - 1) * safePageSize,
                limit = safePageSize
            )
        }
        return Page(page = safePage, pageSize = safePageSize, count = count, records = records)
    }

    fun exportAgreements(userId: String, filterUser: String?): String {
        requirePlatformUser(userId)
        val keyword = filterUser?.trim()?.takeIf { it.isNotEmpty() }
        val records = conventionDao.listAgreements(
            dslContext = dslContext,
            userKeyword = keyword,
            offset = 0,
            limit = ConventionSupport.EXPORT_LIMIT
        )
        val builder = StringBuilder()
        builder.append('\uFEFF')
        builder.append("用户,签署时间\n")
        records.forEach { item ->
            builder.append(csv(item.userId)).append(',')
            builder.append(csv(item.agreeTime)).append('\n')
        }
        return builder.toString()
    }

    private fun visibleContent(lang: String): String? {
        val selected = conventionDao.getBody(dslContext, lang)?.takeUnless { ConventionSupport.blankContent(it) }
        if (selected != null) {
            return selected
        }
        if (lang == ConventionSupport.LANG_ZH) {
            return null
        }
        return conventionDao.getBody(dslContext, ConventionSupport.LANG_ZH)
            ?.takeUnless { ConventionSupport.blankContent(it) }
    }

    private fun requirePlatformUser(userId: String) {
        requireUser(userId)
        if (!authPlatformApi.validateUserPlatformPermission(userId)) {
            throw PermissionForbiddenException("没有平台管理权限")
        }
    }

    private fun requireUser(userId: String) {
        if (userId.isBlank()) {
            throw ErrorCodeException(
                errorCode = ProjectMessageCode.CONVENTION_USER_EMPTY,
                defaultMessage = "User cannot be empty"
            )
        }
    }

    private fun <T> withLock(action: () -> T): T {
        val lock = RedisLock(redisOperation, LOCK_KEY, LOCK_SECONDS)
        lock.lock()
        try {
            return action()
        } finally {
            lock.unlock()
        }
    }

    private fun isDuplicate(exception: DataAccessException): Boolean {
        var cause: Throwable? = exception
        while (cause != null) {
            if (cause is DuplicateKeyException) {
                return true
            }
            val message = cause.message.orEmpty()
            if (message.contains("Duplicate", ignoreCase = true)) {
                return true
            }
            cause = cause.cause
        }
        return false
    }

    private fun csv(value: String): String {
        val safe = if (value.startsWith("=") || value.startsWith("+") ||
            value.startsWith("-") || value.startsWith("@")
        ) {
            "'$value"
        } else {
            value
        }
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }

    companion object {
        private const val LOCK_KEY = "convention:body:lock"
        private const val LOCK_SECONDS = 15L
        private const val DEFAULT_PAGE = 1
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 100
        private val logger = LoggerFactory.getLogger(ConventionService::class.java)
    }
}
