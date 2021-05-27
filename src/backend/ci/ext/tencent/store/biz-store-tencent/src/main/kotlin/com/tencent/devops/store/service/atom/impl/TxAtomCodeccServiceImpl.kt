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

package com.tencent.devops.store.service.atom.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.dao.common.StoreReleaseDao
import com.tencent.devops.store.pojo.atom.UpdateAtomInfo
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.STORE_REPO_CODECC_BUILD_KEY_PREFIX
import com.tencent.devops.store.pojo.common.STORE_REPO_COMMIT_KEY_PREFIX
import com.tencent.devops.store.pojo.common.StoreReleaseCreateRequest
import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.AtomNotifyService
import com.tencent.devops.store.service.atom.AtomQualityService
import com.tencent.devops.store.service.atom.MarketAtomCommonService
import com.tencent.devops.store.service.common.TxStoreCodeccCommonService
import com.tencent.devops.store.service.common.TxStoreCodeccService
import com.tencent.devops.store.service.websocket.StoreWebsocketService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service(value = "ATOM_CODECC_COMMON_SERVICE")
@RefreshScope
class TxAtomCodeccServiceImpl @Autowired constructor() : TxStoreCodeccCommonService {

    private val logger = LoggerFactory.getLogger(TxAtomCodeccServiceImpl::class.java)

    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var redisOperation: RedisOperation

    @Autowired
    private lateinit var marketAtomDao: MarketAtomDao

    @Autowired
    private lateinit var storeReleaseDao: StoreReleaseDao

    @Autowired
    private lateinit var marketAtomCommonService: MarketAtomCommonService

    @Autowired
    private lateinit var atomNotifyService: AtomNotifyService

    @Autowired
    private lateinit var atomQualityService: AtomQualityService

    @Autowired
    private lateinit var storeWebsocketService: StoreWebsocketService

    @Autowired
    private lateinit var txStoreCodeccService: TxStoreCodeccService

    @Value("\${store.codecc.timeout:60}")
    private lateinit var codeccTimeout: String

    override fun doStartTaskAfterOperation(userId: String, storeCode: String, storeId: String?) {
        logger.info("getCodeccMeasureInfo userId:$userId,storeCode:$storeCode,storeId:$storeId")
        if (storeId != null) {
            val atomStatus = AtomStatusEnum.CODECCING.status.toByte()
            doAtomCodeccOperation(storeId, atomStatus, userId)
        }
    }

    override fun doStoreCodeccOperation(
        qualifiedFlag: Boolean,
        storeId: String,
        storeCode: String,
        userId: String
    ) {
        val atomRecord = marketAtomDao.getAtomRecordById(dslContext, storeId)
            ?: throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(storeId)
            )
        val atomStatus = atomRecord.atomStatus
        if (atomStatus != AtomStatusEnum.CODECCING.status.toByte()) {
            // 如果插件状态不是“代码检查中”则直接返回
            return
        }
        val updateTime = atomRecord.updateTime
        // 判断处于“代码检查中”状态的插件是否超过最大轮询时间
        if ((System.currentTimeMillis() - updateTime.timestampmilli()) > codeccTimeout.toInt() * 60 * 1000) {
            doAtomCodeccOperation(storeId, AtomStatusEnum.CODECC_FAIL.status.toByte(), userId)
            return
        }
        val storeType = StoreTypeEnum.ATOM.name
        val codeccFlag = txStoreCodeccService.getCodeccFlag(storeType)
        val validateFlag = if (codeccFlag != null && !codeccFlag) {
            true
        } else {
            qualifiedFlag
        }
        val isNormalUpgrade =
            marketAtomCommonService.getNormalUpgradeFlag(storeCode, atomStatus.toInt())
        val atomFinalStatus = if (!validateFlag) {
            AtomStatusEnum.CODECC_FAIL.status.toByte()
        } else {
            if (isNormalUpgrade) AtomStatusEnum.RELEASED.status.toByte() else AtomStatusEnum.AUDITING.status.toByte()
        }
        if (validateFlag && isNormalUpgrade) {
            // 更新质量红线信息
            atomQualityService.updateQualityInApprove(storeCode, atomFinalStatus)
            dslContext.transaction { t ->
                val context = DSL.using(t)
                // 清空旧版本LATEST_FLAG
                marketAtomDao.cleanLatestFlag(context, storeCode)
                // 记录发布信息
                val pubTime = LocalDateTime.now()
                storeReleaseDao.addStoreReleaseInfo(
                    dslContext = context,
                    userId = userId,
                    storeReleaseCreateRequest = StoreReleaseCreateRequest(
                        storeCode = storeCode,
                        storeType = StoreTypeEnum.ATOM,
                        latestUpgrader = userId,
                        latestUpgradeTime = pubTime
                    )
                )
                marketAtomDao.updateAtomInfoById(
                    dslContext = context,
                    userId = userId,
                    atomId = storeId,
                    updateAtomInfo = UpdateAtomInfo(
                        atomStatus = atomFinalStatus,
                        latestFlag = true,
                        pubTime = pubTime
                    )
                )
                // 处理插件缓存
                marketAtomCommonService.handleAtomCache(
                    atomId = storeId,
                    atomCode = storeCode,
                    version = atomRecord.version,
                    releaseFlag = atomStatus == AtomStatusEnum.RELEASED.status.toByte()
                )
                // 通过websocket推送状态变更消息
                storeWebsocketService.sendWebsocketMessage(userId, storeId)
            }
            // 发送版本发布邮件
            atomNotifyService.sendAtomReleaseAuditNotifyMessage(storeId, AuditTypeEnum.AUDIT_SUCCESS)
        } else {
            marketAtomDao.setAtomStatusById(
                dslContext = dslContext,
                atomId = storeId,
                atomStatus = atomFinalStatus,
                userId = userId,
                msg = ""
            )
            // 通过websocket推送状态变更消息
            storeWebsocketService.sendWebsocketMessage(userId, storeId)
        }
        if (validateFlag) {
            // 清空redis中保存的发布过程保存的buildId和commitId
            redisOperation.delete("$STORE_REPO_COMMIT_KEY_PREFIX:$storeType:$storeCode:$storeId")
            redisOperation.delete("$STORE_REPO_CODECC_BUILD_KEY_PREFIX:$storeType:$storeCode:$storeId")
        }
    }

    private fun doAtomCodeccOperation(storeId: String, atomStatus: Byte, userId: String) {
        marketAtomDao.setAtomStatusById(
            dslContext = dslContext,
            atomId = storeId,
            atomStatus = atomStatus,
            userId = userId,
            msg = ""
        )
        storeWebsocketService.sendWebsocketMessage(userId, storeId)
    }
}
