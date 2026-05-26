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

package com.tencent.devops.store.atom.service.impl

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.atom.dao.AtomWhitelistDao
import com.tencent.devops.store.common.service.AtomWhitelistService
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.AtomWhitelist
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AtomWhitelistServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val atomWhitelistDao: AtomWhitelistDao
) : AtomWhitelistService {
    companion object {
        private val logger = LoggerFactory.getLogger(AtomWhitelistServiceImpl::class.java)
        private const val CACHE_EXPIRE_MINUTES = 5L
    }

    // key = "whitelistType", value = enabled atom codes
    private val whitelistCache: Cache<String, List<String>> = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
        .build()

    override fun isAtomInWhitelist(atomCode: String, whitelistType: String): Boolean {
        return try {
            getAtomCodesByType(whitelistType).contains(atomCode)
        } catch (e: Throwable) {
            logger.warn("isAtomInWhitelist failed|atomCode=$atomCode|type=$whitelistType", e)
            false
        }
    }

    override fun getAtomCodesByType(whitelistType: String): List<String> {
        return try {
            whitelistCache.get(whitelistType) {
                atomWhitelistDao.getAtomCodesByType(
                    dslContext = dslContext,
                    whitelistType = whitelistType
                )
            } ?: emptyList()
        } catch (e: Throwable) {
            logger.warn("getAtomCodesByType failed|type=$whitelistType", e)
            emptyList()
        }
    }

    override fun addOrUpdate(
        whitelistType: String,
        atomCodes: List<String>,
        description: String?,
        operator: String
    ): Result<Boolean> {
        val count = atomWhitelistDao.countWhitelists(
            dslContext = dslContext,
            whitelistType = whitelistType
        )
        val success = if (count > 0) {
            atomWhitelistDao.updateAtomCodes(
                dslContext = dslContext,
                whitelistType = whitelistType,
                atomCodes = atomCodes,
                userId = operator,
                description = description
            )
        } else {
            atomWhitelistDao.addWhitelist(
                dslContext = dslContext,
                whitelistType = whitelistType,
                atomCodes = atomCodes,
                description = description,
                userId = operator
            )
        }
        if (success) {
            invalidateCache(whitelistType)
            logger.info("addOrUpdate success|whitelistType=$whitelistType|operator=$operator")
        }
        return Result(success)
    }

    override fun delete(whitelistType: String, operator: String): Result<Boolean> {
        val count = atomWhitelistDao.countWhitelists(
            dslContext = dslContext,
            whitelistType = whitelistType
        )
        if (count == 0L) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.WHITELIST_NOT_FOUND,
                params = arrayOf(whitelistType)
            )
        }
        val success = atomWhitelistDao.deleteWhitelist(
            dslContext = dslContext,
            whitelistType = whitelistType
        )
        if (success) {
            invalidateCache(whitelistType)
            logger.info("delete success|whitelistType=$whitelistType|operator=$operator")
        }
        return Result(success)
    }

    override fun enableOrDisable(whitelistType: String, enabled: Boolean, operator: String): Result<Boolean> {
        val count = atomWhitelistDao.countWhitelists(
            dslContext = dslContext,
            whitelistType = whitelistType
        )
        if (count == 0L) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.WHITELIST_NOT_FOUND,
                params = arrayOf(whitelistType)
            )
        }
        val success = atomWhitelistDao.updateWhitelistStatus(
            dslContext = dslContext,
            whitelistType = whitelistType,
            enabled = enabled,
            userId = operator
        )
        if (success) {
            invalidateCache(whitelistType)
            logger.info("enableOrDisable success|whitelistType=$whitelistType|enabled=$enabled")
        }
        return Result(success)
    }

    override fun list(
        whitelistType: String?,
        enabled: Boolean?,
        page: Int,
        pageSize: Int
    ): Result<Page<AtomWhitelist>> {
        val atomWhitelists = atomWhitelistDao.listWhitelists(
            dslContext = dslContext,
            whitelistType = whitelistType,
            enabled = enabled,
            page = page,
            pageSize = pageSize
        )
        val count = atomWhitelistDao.countWhitelists(
            dslContext = dslContext,
            whitelistType = whitelistType,
            enabled = enabled
        )
        return Result(Page(page, pageSize, count, atomWhitelists))
    }

    private fun invalidateCache(whitelistType: String) {
        whitelistCache.invalidate(whitelistType)
    }
}