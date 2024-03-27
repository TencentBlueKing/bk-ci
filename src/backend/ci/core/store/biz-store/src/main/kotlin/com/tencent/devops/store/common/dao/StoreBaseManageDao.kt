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

package com.tencent.devops.store.common.dao

import com.tencent.devops.model.store.tables.TStoreBase
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.publication.StoreBaseDataPO
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class StoreBaseManageDao {

    fun saveStoreBaseData(
        dslContext: DSLContext,
        storeBaseDataPO: StoreBaseDataPO
    ) {
        with(TStoreBase.T_STORE_BASE) {
            dslContext.insertInto(
                this,
                ID,
                STORE_CODE,
                STORE_TYPE,
                NAME,
                VERSION,
                STATUS,
                STATUS_MSG,
                LOGO_URL,
                LATEST_FLAG,
                PUBLISHER,
                PUB_TIME,
                CLASSIFY_ID,
                CREATOR,
                MODIFIER,
                UPDATE_TIME,
                CREATE_TIME
            ).values(
                storeBaseDataPO.id,
                storeBaseDataPO.storeCode,
                storeBaseDataPO.storeType.type.toByte(),
                storeBaseDataPO.name,
                storeBaseDataPO.version,
                storeBaseDataPO.status.name,
                storeBaseDataPO.statusMsg,
                storeBaseDataPO.logoUrl,
                storeBaseDataPO.latestFlag,
                storeBaseDataPO.publisher,
                storeBaseDataPO.pubTime,
                storeBaseDataPO.classifyId,
                storeBaseDataPO.creator,
                storeBaseDataPO.modifier,
                storeBaseDataPO.updateTime,
                storeBaseDataPO.createTime
            )
                .onDuplicateKeyUpdate()
                .set(NAME, storeBaseDataPO.name)
                .set(VERSION, storeBaseDataPO.version)
                .set(STATUS, storeBaseDataPO.status.name)
                .set(STATUS_MSG, storeBaseDataPO.statusMsg)
                .set(LOGO_URL, storeBaseDataPO.logoUrl)
                .set(LATEST_FLAG, storeBaseDataPO.latestFlag)
                .set(PUBLISHER, storeBaseDataPO.publisher)
                .set(PUB_TIME, storeBaseDataPO.pubTime)
                .set(CLASSIFY_ID, storeBaseDataPO.classifyId)
                .set(MODIFIER, storeBaseDataPO.modifier)
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun updateStoreStatusById(
        dslContext: DSLContext,
        userId: String,
        storeId: String,
        status: StoreStatusEnum,
        msg: String? = null
    ) {
        with(TStoreBase.T_STORE_BASE) {
            val baseStep = dslContext.update(this)
                .set(STATUS, status.name)
            if (!msg.isNullOrEmpty()) {
                baseStep.set(STATUS_MSG, msg)
            }
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(storeId))
                .execute()
        }
    }
}
