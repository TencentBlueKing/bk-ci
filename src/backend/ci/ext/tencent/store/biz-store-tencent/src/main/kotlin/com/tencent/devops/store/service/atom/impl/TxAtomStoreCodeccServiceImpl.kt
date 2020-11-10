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

package com.tencent.devops.store.service.atom.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.service.common.TxStoreCodeccCommonService
import com.tencent.devops.store.service.websocket.StoreWebsocketService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service(value = "ATOM_CODECC_COMMON_SERVICE")
class TxAtomStoreCodeccServiceImpl @Autowired constructor() : TxStoreCodeccCommonService {

    private val logger = LoggerFactory.getLogger(TxAtomStoreCodeccServiceImpl::class.java)

    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var marketAtomDao: MarketAtomDao

    @Autowired
    private lateinit var storeWebsocketService: StoreWebsocketService

    override fun doStartTaskAfterOperation(userId: String, storeCode: String, storeId: String?) {
        logger.info("getCodeccMeasureInfo userId:$userId,storeCode:$storeCode,storeId:$storeId")
        if (storeId != null) {
            val atomStatus = AtomStatusEnum.CODECCING.status.toByte()
            doAtomCodeccAfterOperation(storeId, atomStatus, userId)
        }
    }

    private fun doAtomCodeccAfterOperation(storeId: String, atomStatus: Byte, userId: String) {
        marketAtomDao.setAtomStatusById(
            dslContext = dslContext,
            atomId = storeId,
            atomStatus = atomStatus,
            userId = userId,
            msg = ""
        )
        storeWebsocketService.sendWebsocketMessage(userId, storeId)
    }

    override fun doGetMeasureInfoAfterOperation(
        userId: String,
        storeCode: String,
        qualifiedFlag: Boolean,
        storeId: String?
    ) {
        logger.info("doGetMeasureInfoAfterOperation userId:$userId,storeCode:$storeCode,qualifiedFlag:$qualifiedFlag,storeId:$storeId")
        if (storeId != null) {
            val atomRecord = marketAtomDao.getAtomById(dslContext, storeId)
                ?: throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf(storeId)
                )
            val dbAtomStatus = atomRecord["atomStatus"] as Byte
            val atomStatus = if (!qualifiedFlag) {
                AtomStatusEnum.CODECC_FAIL.status.toByte()
            } else {
                val releaseTotalNum = marketAtomDao.countReleaseAtomByCode(dslContext, storeCode)
                val currentNum = if (dbAtomStatus == AtomStatusEnum.RELEASED.status.toByte()) 1 else 0
                val isNormalUpgrade = releaseTotalNum > currentNum
                if (isNormalUpgrade) AtomStatusEnum.RELEASED.status.toByte() else AtomStatusEnum.AUDITING.status.toByte()
            }
            if (atomStatus != dbAtomStatus) {
                doAtomCodeccAfterOperation(storeId, atomStatus, userId)
            }
        }
    }
}
