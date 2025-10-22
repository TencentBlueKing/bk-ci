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

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.store.tables.TStoreSensitiveApi
import com.tencent.devops.model.store.tables.records.TStoreSensitiveApiRecord
import com.tencent.devops.store.pojo.common.sensitive.SensitiveApiCreateDTO
import com.tencent.devops.store.pojo.common.sensitive.SensitiveApiInfo
import com.tencent.devops.store.pojo.common.sensitive.SensitiveApiSearchDTO
import com.tencent.devops.store.pojo.common.sensitive.SensitiveApiUpdateDTO
import com.tencent.devops.store.pojo.common.enums.ApiStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class SensitiveApiDao {

    fun create(
        dslContext: DSLContext,
        sensitiveApiCreateDTOs: List<SensitiveApiCreateDTO>
    ) {
        val now = LocalDateTime.now()
        val records = sensitiveApiCreateDTOs.map { dto ->
            with(dto) {
                with(TStoreSensitiveApi.T_STORE_SENSITIVE_API) {
                    dslContext.insertInto(
                        this,
                        ID,
                        STORE_CODE,
                        STORE_TYPE,
                        API_NAME,
                        ALIAS_NAME,
                        API_STATUS,
                        API_LEVEL,
                        APPLY_DESC,
                        CREATOR,
                        MODIFIER,
                        CREATE_TIME,
                        UPDATE_TIME
                    ).values(
                        id,
                        storeCode,
                        storeType.type.toByte(),
                        apiName,
                        aliasName,
                        apiStatus.name,
                        apiLevel.name,
                        applyDesc,
                        userId,
                        userId,
                        now,
                        now
                    ).onDuplicateKeyUpdate()
                        .set(API_STATUS, apiStatus.name)
                        .set(API_LEVEL, apiLevel.name)
                        .set(APPLY_DESC, applyDesc)
                        .set(UPDATE_TIME, now)
                }
            }
        }
        dslContext.batch(records).execute()
    }

    fun get(
        dslContext: DSLContext,
        id: String
    ): TStoreSensitiveApiRecord? {
        return with(TStoreSensitiveApi.T_STORE_SENSITIVE_API) {
            dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun getByApiName(
        dslContext: DSLContext,
        storeType: StoreTypeEnum,
        storeCode: String,
        apiName: String
    ): TStoreSensitiveApiRecord? {
        return with(TStoreSensitiveApi.T_STORE_SENSITIVE_API) {
            dslContext.selectFrom(this)
                .where(STORE_TYPE.eq(storeType.type.toByte()))
                .and(STORE_CODE.eq(storeCode))
                .and(API_NAME.eq(apiName))
                .fetchOne()
        }
    }

    fun getApprovedApiNameList(
        dslContext: DSLContext,
        storeType: StoreTypeEnum,
        storeCode: String
    ): List<String> {
        return with(TStoreSensitiveApi.T_STORE_SENSITIVE_API) {
            dslContext.select(API_NAME).from(this)
                .where(STORE_TYPE.eq(storeType.type.toByte()))
                .and(STORE_CODE.eq(storeCode))
                .and(API_STATUS.eq(ApiStatusEnum.PASS.name))
                .fetch(API_NAME)
        }
    }

    fun list(
        dslContext: DSLContext,
        offset: Int,
        limit: Int,
        sensitiveApiSearchDTO: SensitiveApiSearchDTO
    ): Result<TStoreSensitiveApiRecord> {
        return with(TStoreSensitiveApi.T_STORE_SENSITIVE_API) {
            val conditions = queryCondition(sensitiveApiSearchDTO)
            dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(CREATE_TIME.desc())
                .limit(offset, limit)
                .skipCheck()
                .fetch()
        }
    }

    fun count(
        dslContext: DSLContext,
        sensitiveApiSearchDTO: SensitiveApiSearchDTO
    ): Long {
        return with(TStoreSensitiveApi.T_STORE_SENSITIVE_API) {
            val conditions = queryCondition(sensitiveApiSearchDTO)
            dslContext.selectCount().from(this)
                .where(conditions)
                .skipCheck()
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun updateApiStatus(
        dslContext: DSLContext,
        sensitiveApiUpdateDTO: SensitiveApiUpdateDTO
    ) {
        with(sensitiveApiUpdateDTO) {
            with(TStoreSensitiveApi.T_STORE_SENSITIVE_API) {
                val update = dslContext.update(this)
                    .set(API_STATUS, apiStatus.name)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .set(MODIFIER, userId)
                if (approveMsg != null) {
                    update.set(APPROVE_MSG, approveMsg)
                }
                update.where(ID.eq(id)).execute()
            }
        }
    }

    private fun TStoreSensitiveApi.queryCondition(
        sensitiveApiSearchDTO: SensitiveApiSearchDTO
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        return with(sensitiveApiSearchDTO) {
            if (storeType != null) {
                conditions.add(STORE_TYPE.eq(storeType!!.type.toByte()))
            }
            if (!storeCode.isNullOrBlank()) {
                conditions.add(STORE_CODE.eq(storeCode))
            }
            if (!apiName.isNullOrBlank()) {
                conditions.add(API_NAME.eq(apiName))
            }
            if (apiLevel != null) {
                conditions.add(API_LEVEL.eq(apiLevel!!.name))
            }
            if (apiStatus != null) {
                conditions.add(API_STATUS.eq(apiStatus!!.name))
            }
            conditions
        }
    }

    fun convert(record: TStoreSensitiveApiRecord): SensitiveApiInfo {
        return with(record) {
            SensitiveApiInfo(
                id = id,
                storeType = StoreTypeEnum.getStoreTypeObj(storeType.toInt())!!,
                storeCode = storeCode,
                apiName = apiName,
                aliasName = aliasName,
                apiLevel = apiLevel,
                apiStatus = apiStatus,
                applyDesc = applyDesc,
                approveMsg = approveMsg,
                creator = creator,
                modifier = modifier,
                createTime = createTime.timestampmilli(),
                updateTime = updateTime.timestampmilli()
            )
        }
    }
}
