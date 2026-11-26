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

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.store.tables.records.TStoreComponentContractRecord
import com.tencent.devops.store.atom.service.AtomSelectionIndexCache
import com.tencent.devops.store.common.dao.StoreComponentContractDao
import com.tencent.devops.store.common.utils.StoreComponentContractUtils
import com.tencent.devops.store.common.utils.StoreComponentContractUtils.WriteAction
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractBrief
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractCoverageEnum
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractDescription
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractDescriptionWriteReq
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractInfo
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractStaleReasonEnum
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractViewEnum
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractWriteReq
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractWriteResultEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreComponentContractService @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeComponentContractDao: StoreComponentContractDao,
    private val atomSelectionIndexCache: AtomSelectionIndexCache,
    private val storeComponentContractExtractService: StoreComponentContractExtractService
) {

    fun writeContract(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        writeReq: StoreComponentContractWriteReq
    ): StoreComponentContractWriteResultEnum {
        validateWriteReq(storeCode, writeReq)
        val result = dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            val existing = storeComponentContractDao.getContract(
                dslContext = context,
                storeType = storeType,
                storeCode = storeCode,
                forUpdate = true
            )
            val action = StoreComponentContractUtils.decideWrite(
                existing = existing?.let { toState(it) },
                basedOnVersion = writeReq.basedOnVersion,
                sourceCommit = writeReq.sourceCommit,
                propsHash = writeReq.propsHash
            )
            applyWrite(context, userId, action, existing, storeType, storeCode, writeReq)
        }
        if (storeType == StoreTypeEnum.ATOM) {
            atomSelectionIndexCache.invalidate()
        }
        if (result == StoreComponentContractWriteResultEnum.CREATED) {
            storeComponentContractExtractService.clearMissingFailCount(storeType, storeCode, writeReq.basedOnVersion)
        }
        storeComponentContractExtractService.releaseLease(storeType, storeCode)
        logger.info(
            "contract write|$userId|$storeType|$storeCode|${writeReq.basedOnVersion}|" +
                "${writeReq.extractBuildId}|$result"
        )
        return result
    }

    fun verifyContract(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        basedOnVersion: String?
    ): Boolean {
        val record = storeComponentContractDao.getContract(dslContext, storeType, storeCode)
            ?: throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(storeCode)
            )
        if (!basedOnVersion.isNullOrBlank() && basedOnVersion != record.basedOnVersion) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("basedOnVersion:$basedOnVersion")
            )
        }
        storeComponentContractDao.verifyContract(dslContext, userId, storeType, storeCode)
        logger.info("contract verified|$userId|$storeType|$storeCode|${record.basedOnVersion}")
        return true
    }

    fun getContract(
        storeType: StoreTypeEnum,
        storeCode: String,
        view: StoreComponentContractViewEnum
    ): StoreComponentContractInfo? {
        val record = storeComponentContractDao.getContract(dslContext, storeType, storeCode) ?: return null
        val contract = JsonUtil.to(record.contract, object : TypeReference<Map<String, Any?>>() {})
        val fullView = view == StoreComponentContractViewEnum.FULL
        return StoreComponentContractInfo(
            storeCode = record.storeCode,
            storeType = storeType.name,
            view = view,
            schemaVersion = record.schemaVersion,
            basedOnVersion = record.basedOnVersion,
            sourceCommit = record.sourceCommit,
            propsHash = record.propsHash,
            coverage = record.coverage,
            staleFlag = record.staleFlag == true,
            staleReason = record.staleReason,
            lastSeenVersion = record.lastSeenVersion,
            selectionSummary = record.selectionSummary,
            selectionKeywords = record.selectionKeywords?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
            contract = if (fullView) contract else StoreComponentContractUtils.buildCardContract(contract),
            generatedDescription = if (fullView) record.generatedDescription else null,
            reportPath = if (fullView) record.reportPath else null,
            extractBuildId = if (fullView) record.extractBuildId else null,
            reviewer = record.reviewer,
            reviewTime = record.reviewTime,
            updateTime = record.updateTime
        )
    }

    /**
     * 只更新运行时说明；生成说明期间契约被覆盖时版本或提交对不上，返回 false，由调用方跳过
     */
    fun writeDescription(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        writeReq: StoreComponentContractDescriptionWriteReq
    ): Boolean {
        if (!StoreComponentContractUtils.isValidVersion(writeReq.basedOnVersion)) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("basedOnVersion:${writeReq.basedOnVersion}")
            )
        }
        val count = storeComponentContractDao.updateDescription(
            dslContext = dslContext,
            userId = userId,
            storeType = storeType,
            storeCode = storeCode,
            basedOnVersion = writeReq.basedOnVersion,
            sourceCommit = writeReq.sourceCommit,
            description = writeReq.description.ifBlank { null }
        )
        if (count == 0 && storeComponentContractDao.getContract(dslContext, storeType, storeCode) == null) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(storeCode)
            )
        }
        logger.info(
            "contract description write|$userId|$storeType|$storeCode|${writeReq.basedOnVersion}|" +
                "${writeReq.sourceCommit}|${writeReq.extractBuildId}|${writeReq.description.length}|$count"
        )
        return count > 0
    }

    fun listContracts(storeType: StoreTypeEnum): List<StoreComponentContractBrief> {
        return storeComponentContractDao.listBriefs(dslContext, storeType)
    }

    fun getDescription(storeType: StoreTypeEnum, storeCode: String): StoreComponentContractDescription? {
        val record = storeComponentContractDao.getContract(dslContext, storeType, storeCode) ?: return null
        val description = record.generatedDescription?.takeIf { it.isNotBlank() } ?: return null
        return StoreComponentContractDescription(
            storeCode = record.storeCode,
            basedOnVersion = record.basedOnVersion,
            coverage = record.coverage,
            staleFlag = record.staleFlag == true,
            description = description,
            updateTime = record.updateTime
        )
    }

    /**
     * 组件发布成为最新版本后标记契约过期，失败不影响发布
     */
    fun markStaleOnRelease(storeType: StoreTypeEnum, storeCode: String, version: String) {
        try {
            val count = storeComponentContractDao.markStaleOnRelease(
                dslContext = dslContext,
                storeType = storeType,
                storeCode = storeCode,
                staleReason = StoreComponentContractStaleReasonEnum.NEW_VERSION.name,
                version = version
            )
            logger.info("contract mark stale on release|$storeType|$storeCode|$version|$count")
        } catch (ignored: Throwable) {
            logger.warn("contract mark stale on release failed|$storeType|$storeCode|$version", ignored)
        }
    }

    private fun validateWriteReq(storeCode: String, writeReq: StoreComponentContractWriteReq) {
        if (writeReq.coverage == StoreComponentContractCoverageEnum.VERIFIED) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("coverage:${writeReq.coverage}")
            )
        }
        // 选型关键词以英文逗号拼接入库
        writeReq.selectionKeywords?.firstOrNull { it.contains(",") }?.let {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("selectionKeywords:$it")
            )
        }
        if (!StoreComponentContractUtils.isValidVersion(writeReq.basedOnVersion)) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("basedOnVersion:${writeReq.basedOnVersion}")
            )
        }
        val contract = try {
            JsonUtil.to(writeReq.contract, object : TypeReference<Map<String, Any?>>() {})
        } catch (ignored: Exception) {
            null
        }
        if (contract == null || contract["atom_code"] != storeCode) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("contract")
            )
        }
    }

    @Suppress("LongParameterList")
    private fun applyWrite(
        context: DSLContext,
        userId: String,
        action: WriteAction,
        existing: TStoreComponentContractRecord?,
        storeType: StoreTypeEnum,
        storeCode: String,
        writeReq: StoreComponentContractWriteReq
    ): StoreComponentContractWriteResultEnum {
        return when (action) {
            WriteAction.CREATE -> {
                storeComponentContractDao.createContract(context, userId, storeType, storeCode, writeReq)
                StoreComponentContractWriteResultEnum.CREATED
            }
            WriteAction.OVERWRITE -> {
                val lastSeenVersion = existing?.lastSeenVersion
                val keepStale = StoreComponentContractUtils.keepStaleAfterOverwrite(
                    lastSeenVersion = lastSeenVersion,
                    basedOnVersion = writeReq.basedOnVersion
                )
                storeComponentContractDao.overwriteContract(
                    dslContext = context,
                    userId = userId,
                    storeType = storeType,
                    storeCode = storeCode,
                    writeReq = writeReq,
                    staleFlag = keepStale,
                    lastSeenVersion = lastSeenVersion?.takeIf { keepStale } ?: writeReq.basedOnVersion
                )
                StoreComponentContractWriteResultEnum.UPDATED
            }
            WriteAction.MARK_PENDING_COMPARE -> {
                val reason = StoreComponentContractStaleReasonEnum.VERIFIED_PENDING_COMPARE.name
                storeComponentContractDao.markPendingCompare(
                    dslContext = context,
                    userId = userId,
                    storeType = storeType,
                    storeCode = storeCode,
                    staleReason = writeReq.extractBuildId?.takeIf { it.isNotBlank() }?.let { "$reason:$it" } ?: reason
                )
                StoreComponentContractWriteResultEnum.KEPT_VERIFIED
            }
            WriteAction.SKIP_VERIFIED -> StoreComponentContractWriteResultEnum.KEPT_VERIFIED
            WriteAction.SKIP_OUTDATED -> StoreComponentContractWriteResultEnum.SKIPPED_OUTDATED
        }
    }

    private fun toState(record: TStoreComponentContractRecord) = StoreComponentContractUtils.ContractState(
        basedOnVersion = record.basedOnVersion,
        sourceCommit = record.sourceCommit,
        propsHash = record.propsHash,
        coverage = record.coverage,
        lastSeenVersion = record.lastSeenVersion
    )

    companion object {
        private val logger = LoggerFactory.getLogger(StoreComponentContractService::class.java)
    }
}
