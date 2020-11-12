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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.service.atom

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.plugin.api.ServiceCodeccResource
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.dao.common.StoreReleaseDao
import com.tencent.devops.store.pojo.atom.UpdateAtomInfo
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.STORE_REPO_CODECC_BUILD_KEY_PREFIX
import com.tencent.devops.store.pojo.common.STORE_REPO_COMMIT_KEY_PREFIX
import com.tencent.devops.store.pojo.common.StoreReleaseCreateRequest
import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.TxStoreCodeccService
import com.tencent.devops.store.service.websocket.StoreWebsocketService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class TxAtomCronService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val marketAtomDao: MarketAtomDao,
    private val storeReleaseDao: StoreReleaseDao,
    private val marketAtomCommonService: MarketAtomCommonService,
    private val atomNotifyService: AtomNotifyService,
    private val atomQualityService: AtomQualityService,
    private val storeWebsocketService: StoreWebsocketService,
    private val txStoreCodeccService: TxStoreCodeccService
) {

    private val logger = LoggerFactory.getLogger(TxAtomCronService::class.java)

    @Value("\${git.plugin.nameSpaceName}")
    private lateinit var pluginNameSpaceName: String

    @Scheduled(cron = "0 */2 * * * ?")
    fun updateAtomCodeccStatus() {
        // 一次最多查30条处于“代码检查中”状态的插件数据
        val redisLock = RedisLock(redisOperation, "updateAtomCodeccStatus", 60L)
        try {
            val lockSuccess = redisLock.tryLock()
            if (!lockSuccess) {
                logger.info("updateAtomCodeccStatus is running")
                return
            }
            logger.info("updateAtomCodeccStatus start")
            val atomRecords = marketAtomDao.listAtomByStatus(
                dslContext = dslContext,
                atomStatus = AtomStatusEnum.CODECCING.status.toByte(),
                page = 1,
                pageSize = 30,
                timeDescFlag = false
            )
            if (atomRecords == null || atomRecords.isEmpty()) {
                return
            }
            atomRecords.forEach {
                val atomId = it.id
                val atomCode = it.atomCode
                val storeType = StoreTypeEnum.ATOM.name
                // 获取当次构建对应的buildId
                val buildId = redisOperation.get("$STORE_REPO_CODECC_BUILD_KEY_PREFIX:$storeType:$atomCode:$atomId")
                val repoId = "$pluginNameSpaceName/$atomCode"
                val codeccMeasureInfoResult = client.get(ServiceCodeccResource::class).getCodeccMeasureInfo(
                    repoId = repoId,
                    buildId = buildId
                )
                val codeccMeasureInfo = codeccMeasureInfoResult.data
                if (codeccMeasureInfo != null) {
                    val codeStyleScore = codeccMeasureInfo.codeStyleScore
                    val codeSecurityScore = codeccMeasureInfo.codeSecurityScore
                    val codeMeasureScore = codeccMeasureInfo.codeMeasureScore
                    if (codeStyleScore != null && codeSecurityScore != null && codeMeasureScore != null) {
                        if (codeccMeasureInfo.status != 3) {
                            val codeccFlag = txStoreCodeccService.getCodeccFlag(storeType)
                            val validateFlag = if (codeccFlag != null && !codeccFlag) {
                                true
                            } else {
                                txStoreCodeccService.getQualifiedFlag(
                                    storeType = storeType,
                                    codeStyleScore = codeStyleScore,
                                    codeSecurityScore = codeSecurityScore,
                                    codeMeasureScore = codeMeasureScore
                                )
                            }
                            val isNormalUpgrade =
                                marketAtomCommonService.getNormalUpgradeFlag(atomCode, it.atomStatus.toInt())
                            val atomFinalStatus = if (!validateFlag) {
                                AtomStatusEnum.CODECC_FAIL.status.toByte()
                            } else {
                                if (isNormalUpgrade) AtomStatusEnum.RELEASED.status.toByte() else AtomStatusEnum.AUDITING.status.toByte()
                            }
                            val userId = it.modifier
                            if (validateFlag && isNormalUpgrade) {
                                // 更新质量红线信息
                                atomQualityService.updateQualityInApprove(atomCode, atomFinalStatus)
                                dslContext.transaction { t ->
                                    val context = DSL.using(t)
                                    // 清空旧版本LATEST_FLAG
                                    marketAtomDao.cleanLatestFlag(context, atomCode)
                                    // 记录发布信息
                                    val pubTime = LocalDateTime.now()
                                    storeReleaseDao.addStoreReleaseInfo(
                                        dslContext = context,
                                        userId = userId,
                                        storeReleaseCreateRequest = StoreReleaseCreateRequest(
                                            storeCode = atomCode,
                                            storeType = StoreTypeEnum.ATOM,
                                            latestUpgrader = userId,
                                            latestUpgradeTime = pubTime
                                        )
                                    )
                                    marketAtomDao.updateAtomInfoById(
                                        dslContext = context,
                                        userId = userId,
                                        atomId = atomId,
                                        updateAtomInfo = UpdateAtomInfo(
                                            atomStatus = atomFinalStatus,
                                            latestFlag = true,
                                            pubTime = pubTime
                                        )
                                    )
                                    // 通过websocket推送状态变更消息
                                    storeWebsocketService.sendWebsocketMessage(userId, atomId)
                                }
                                // 发送版本发布邮件
                                atomNotifyService.sendAtomReleaseAuditNotifyMessage(atomId, AuditTypeEnum.AUDIT_SUCCESS)
                            } else {
                                marketAtomDao.setAtomStatusById(
                                    dslContext = dslContext,
                                    atomId = atomId,
                                    atomStatus = atomFinalStatus,
                                    userId = userId,
                                    msg = ""
                                )
                                // 通过websocket推送状态变更消息
                                storeWebsocketService.sendWebsocketMessage(userId, atomId)
                            }
                            if (validateFlag) {
                                // 清空redis中保存的发布过程保存的buildId和commitId
                                redisOperation.delete("$STORE_REPO_COMMIT_KEY_PREFIX:$storeType:$atomCode:$atomId")
                                redisOperation.delete("$STORE_REPO_CODECC_BUILD_KEY_PREFIX:$storeType:$atomCode:$atomId")
                            }
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            logger.error("updateAtomCodeccStatus error:", e)
        } finally {
            redisLock.unlock()
        }
    }
}