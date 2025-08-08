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

package com.tencent.devops.store.common.dao

import com.tencent.devops.model.store.tables.TStoreBase
import com.tencent.devops.store.pojo.common.StoreBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreBaseDataPO
import com.tencent.devops.store.pojo.common.publication.UpdateStoreBaseDataPO
import java.time.LocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

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
                SUMMARY,
                DESCRIPTION,
                LATEST_FLAG,
                PUBLISHER,
                PUB_TIME,
                CLASSIFY_ID,
                BUS_NUM,
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
                storeBaseDataPO.summary,
                storeBaseDataPO.description,
                storeBaseDataPO.latestFlag,
                storeBaseDataPO.publisher,
                storeBaseDataPO.pubTime,
                storeBaseDataPO.classifyId,
                storeBaseDataPO.busNum,
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
                .set(SUMMARY, storeBaseDataPO.summary)
                .set(DESCRIPTION, storeBaseDataPO.description)
                .set(LATEST_FLAG, storeBaseDataPO.latestFlag)
                .set(PUBLISHER, storeBaseDataPO.publisher)
                .set(PUB_TIME, storeBaseDataPO.pubTime)
                .set(CLASSIFY_ID, storeBaseDataPO.classifyId)
                .set(BUS_NUM, storeBaseDataPO.busNum)
                .set(MODIFIER, storeBaseDataPO.modifier)
                .set(UPDATE_TIME, storeBaseDataPO.updateTime)
                .execute()
        }
    }

    fun updateStoreBaseInfo(
        dslContext: DSLContext,
        updateStoreBaseDataPO: UpdateStoreBaseDataPO
    ) {
        with(TStoreBase.T_STORE_BASE) {
            val baseStep = dslContext.update(this)
            val name = updateStoreBaseDataPO.name
            if (!name.isNullOrBlank()) {
                baseStep.set(NAME, name)
            }
            val status = updateStoreBaseDataPO.status
            if (status != null) {
                baseStep.set(STATUS, status.name)
            }
            val statusMsg = updateStoreBaseDataPO.statusMsg
            if (!statusMsg.isNullOrBlank()) {
                baseStep.set(STATUS_MSG, statusMsg)
            }
            val logoUrl = updateStoreBaseDataPO.logoUrl
            if (!logoUrl.isNullOrBlank()) {
                baseStep.set(LOGO_URL, logoUrl)
            }
            val summary = updateStoreBaseDataPO.summary
            if (!summary.isNullOrBlank()) {
                baseStep.set(SUMMARY, summary)
            }
            val description = updateStoreBaseDataPO.description
            if (!description.isNullOrBlank()) {
                baseStep.set(DESCRIPTION, description)
            }
            val latestFlag = updateStoreBaseDataPO.latestFlag
            if (latestFlag != null) {
                baseStep.set(LATEST_FLAG, latestFlag)
            }
            val publisher = updateStoreBaseDataPO.publisher
            if (!publisher.isNullOrBlank()) {
                baseStep.set(PUBLISHER, publisher)
            }
            val pubTime = updateStoreBaseDataPO.pubTime
            if (pubTime != null) {
                baseStep.set(PUB_TIME, pubTime)
            }
            val classifyId = updateStoreBaseDataPO.classifyId
            if (!classifyId.isNullOrBlank()) {
                baseStep.set(CLASSIFY_ID, classifyId)
            }
            baseStep.set(MODIFIER, updateStoreBaseDataPO.modifier)
                .set(UPDATE_TIME, updateStoreBaseDataPO.updateTime)
                .where(ID.eq(updateStoreBaseDataPO.id))
                .execute()
        }
    }

    fun cleanLatestFlag(
        dslContext: DSLContext,
        storeCode: String,
        storeType: StoreTypeEnum
    ) {
        with(TStoreBase.T_STORE_BASE) {
            dslContext.update(this)
                .set(LATEST_FLAG, false)
                .where(STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType.type.toByte())))
                .execute()
        }
    }

    @Suppress("LongParameterList")
    fun offlineComponent(
        dslContext: DSLContext,
        storeCode: String,
        storeType: StoreTypeEnum,
        userId: String,
        msg: String? = null,
        latestFlag: Boolean? = null
    ) {
        with(TStoreBase.T_STORE_BASE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_TYPE.eq(storeType.type.toByte()))
            conditions.add(STORE_CODE.eq(storeCode))
            conditions.add(STATUS.eq(StoreStatusEnum.RELEASED.name))
            val baseStep = dslContext.update(this)
                .set(STATUS, StoreStatusEnum.UNDERCARRIAGED.name)
            if (!msg.isNullOrEmpty()) {
                baseStep.set(STATUS_MSG, msg)
            }
            if (null != latestFlag) {
                baseStep.set(LATEST_FLAG, latestFlag)
            }
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(conditions)
                .execute()
        }
    }

    fun updateComponentBaseInfo(
        dslContext: DSLContext,
        userId: String,
        storeIds: List<String>,
        classifyId: String?,
        storeBaseInfoUpdateRequest: StoreBaseInfoUpdateRequest
    ) {
        with(TStoreBase.T_STORE_BASE) {
            val baseStep = dslContext.update(this)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
            classifyId?.let {
                baseStep.set(CLASSIFY_ID, it)
            }
            storeBaseInfoUpdateRequest.summary?.let {
                baseStep.set(SUMMARY, it)
            }
            storeBaseInfoUpdateRequest.description?.let {
                baseStep.set(DESCRIPTION, it)
            }
            storeBaseInfoUpdateRequest.logoUrl?.let {
                baseStep.set(LOGO_URL, it)
            }
            storeBaseInfoUpdateRequest.publisher?.let {
                baseStep.set(PUBLISHER, it)
            }
            if (!storeBaseInfoUpdateRequest.name.isNullOrBlank()) {
                baseStep.set(NAME, storeBaseInfoUpdateRequest.name)
            }
            baseStep.where(ID.`in`(storeIds)).execute()
        }
    }

    fun deleteByComponentCode(dslContext: DSLContext, storeCode: String, storeType: Byte) {
        with(TStoreBase.T_STORE_BASE) {
            dslContext.deleteFrom(this)
                .where(STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType)))
                .execute()
        }
    }
}
