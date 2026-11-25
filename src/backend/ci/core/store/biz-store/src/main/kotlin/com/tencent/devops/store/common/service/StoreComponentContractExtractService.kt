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

package com.tencent.devops.store.common.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.store.atom.dao.AtomContractCandidateDao
import com.tencent.devops.store.common.dao.StoreComponentContractDao
import com.tencent.devops.store.common.service.StoreComponentSourceResolver.AtomRelease
import com.tencent.devops.store.common.utils.StoreComponentContractPendingUtils
import com.tencent.devops.store.common.utils.StoreComponentContractPendingUtils.Candidate
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractPendingReasonEnum
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractPendingResp
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractStaleReasonEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 契约自动抽取的调度支撑：给对账流水线列出待抽取组件、记录抽取失败
 */
@Service
class StoreComponentContractExtractService @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeComponentContractDao: StoreComponentContractDao,
    private val atomContractCandidateDao: AtomContractCandidateDao,
    private val storeComponentSourceResolver: StoreComponentSourceResolver,
    private val redisOperation: RedisOperation
) {

    fun listPending(
        storeType: StoreTypeEnum,
        limit: Int,
        maxFailCount: Int,
        includeMissing: Boolean,
        minRecentExecuteNum: Int,
        leaseMinutes: Int = 0
    ): StoreComponentContractPendingResp {
        checkStoreType(storeType)
        val size = limit.coerceIn(1, MAX_LIMIT)
        val leaseSeconds = leaseMinutes.coerceIn(0, MAX_LEASE_MINUTES) * 60L
        // 开启租约时排在前面的可能都已租出，多扫一批，剔除后才不会扫空
        val scanSize = size + EXTRA_SCAN_SIZE + if (leaseSeconds > 0) LEASE_EXTRA_SCAN_SIZE else 0
        val stale = storeComponentContractDao.listStaleForExtract(
            dslContext = dslContext,
            storeType = storeType,
            staleReason = StoreComponentContractStaleReasonEnum.NEW_VERSION.name,
            maxFailCount = maxFailCount,
            limit = scanSize
        ).map { Candidate(it.value1(), StoreComponentContractPendingReasonEnum.NEW_VERSION, it.value3() ?: 0) }
        val needMissing = includeMissing && stale.size < scanSize
        val existingCodes = if (needMissing) {
            storeComponentContractDao.listStoreCodes(dslContext, storeType)
        } else {
            emptySet()
        }
        // 执行次数靠前的插件可能都已有契约，多取已有契约数量的条数再剔除
        val hotReleases = if (needMissing) {
            atomContractCandidateDao.listHotLatestReleases(
                dslContext = dslContext,
                minRecentExecuteNum = minRecentExecuteNum,
                limit = scanSize + existingCodes.size
            )
        } else {
            emptyList()
        }
        val candidates = StoreComponentContractPendingUtils.mergeCandidates(
            stale = stale,
            hotReleases = hotReleases,
            existingCodes = existingCodes,
            maxFailCount = maxFailCount,
            scanSize = scanSize,
            leased = { storeCode -> leaseSeconds > 0 && redisOperation.hasKey(leaseKey(storeType, storeCode)) }
        ) { release -> getMissingFailCount(storeType, release.atomCode, release.version) }
        val releases = hotReleases.associateBy { it.atomCode } +
            atomContractCandidateDao.listLatestReleases(dslContext, stale.map { it.storeCode })
        val resp = StoreComponentContractPendingUtils.pick(
            candidates = candidates,
            releases = releases,
            limit = size,
            acquire = { release -> acquireLease(storeType, release, leaseSeconds) }
        ) { release ->
            try {
                storeComponentSourceResolver.resolveAtomSource(release)
            } catch (ignored: Throwable) {
                logger.warn("contract pending resolve source failed|${release.atomCode}|${release.version}", ignored)
                null
            }
        }
        logger.info(
            "contract pending|$storeType|lease=${leaseMinutes}m|candidates=${candidates.size}|" +
                "items=${resp.items.map { "${it.storeCode}@${it.version}:${it.refType}" }}|" +
                "skipped=${resp.skipped.map { "${it.storeCode}:${it.reason}" }}"
        )
        return resp
    }

    /**
     * 有契约行时累加契约表的失败次数；没有契约行时记 Redis，key 带版本号，发新版本后自然重新进队列
     */
    fun reportExtractFail(userId: String, storeType: StoreTypeEnum, storeCode: String, version: String): Int {
        checkStoreType(storeType)
        val updated = storeComponentContractDao.increaseExtractFailCount(dslContext, userId, storeType, storeCode)
        val failCount = if (updated > 0) {
            storeComponentContractDao.getContract(dslContext, storeType, storeCode)?.extractFailCount ?: 0
        } else {
            val key = missingFailKey(storeType, storeCode, version)
            val count = redisOperation.increment(key, 1) ?: 0
            redisOperation.expire(key, MISSING_FAIL_EXPIRE_SECONDS)
            count.toInt()
        }
        releaseLease(storeType, storeCode)
        logger.info("contract extract fail|$userId|$storeType|$storeCode|$version|$failCount")
        return failCount
    }

    fun clearMissingFailCount(storeType: StoreTypeEnum, storeCode: String, version: String) {
        redisOperation.delete(missingFailKey(storeType, storeCode, version))
    }

    fun releaseLease(storeType: StoreTypeEnum, storeCode: String) {
        redisOperation.delete(leaseKey(storeType, storeCode))
    }

    /**
     * 候选清单是先查出来的，别的构建可能在这之后写完契约并释放了租约，抢到租约后要按最新的契约再确认一次
     */
    private fun acquireLease(storeType: StoreTypeEnum, release: AtomRelease, leaseSeconds: Long): Boolean {
        if (leaseSeconds <= 0) {
            return true
        }
        val key = leaseKey(storeType, release.atomCode)
        if (!redisOperation.setIfAbsent(key = key, value = release.version, expiredInSecond = leaseSeconds)) {
            return false
        }
        val contract = storeComponentContractDao.getContract(dslContext, storeType, release.atomCode)
        if (contract != null && contract.staleFlag != true && contract.basedOnVersion == release.version) {
            redisOperation.delete(key)
            return false
        }
        return true
    }

    private fun getMissingFailCount(storeType: StoreTypeEnum, storeCode: String, version: String): Int {
        return redisOperation.get(missingFailKey(storeType, storeCode, version))?.toIntOrNull() ?: 0
    }

    private fun checkStoreType(storeType: StoreTypeEnum) {
        if (storeType != StoreTypeEnum.ATOM) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("storeType:$storeType")
            )
        }
    }

    private fun missingFailKey(storeType: StoreTypeEnum, storeCode: String, version: String) =
        "store:contract:extract_fail:$storeType:$storeCode:$version"

    private fun leaseKey(storeType: StoreTypeEnum, storeCode: String) =
        "store:contract:extract_lease:$storeType:$storeCode"

    companion object {
        private val logger = LoggerFactory.getLogger(StoreComponentContractExtractService::class.java)
        private const val MAX_LIMIT = 10
        private const val EXTRA_SCAN_SIZE = 20
        private const val LEASE_EXTRA_SCAN_SIZE = 200
        private const val MAX_LEASE_MINUTES = 24 * 60
        private const val MISSING_FAIL_EXPIRE_SECONDS = 7 * 24 * 3600L
    }
}
