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

package com.tencent.devops.store.dao.common

import com.tencent.devops.model.store.tables.TStoreSensitiveConf
import com.tencent.devops.model.store.tables.records.TStoreSensitiveConfRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class SensitiveConfDao {

    /**
     * 判断是否存在同名数据
     */
    fun check(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte,
        fieldName: String?,
        id: String?
    ): Boolean {
        with(TStoreSensitiveConf.T_STORE_SENSITIVE_CONF) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_CODE.eq(storeCode))
            conditions.add(STORE_TYPE.eq(storeType))
            if (fieldName != null) conditions.add(FIELD_NAME.eq(fieldName))
            if (id != null) conditions.add(ID.notEqual(id))
            return dslContext.selectCount().from(this).where(conditions).fetchOne(0, Int::class.java) > 0
        }
    }

    /**
     * 新增敏感配置
     */
    fun create(
        dslContext: DSLContext,
        userId: String,
        id: String,
        storeCode: String,
        storeType: Byte,
        fieldName: String,
        fieldValue: String,
        fieldDesc: String?
    ) {
        with(TStoreSensitiveConf.T_STORE_SENSITIVE_CONF) {
            dslContext.insertInto(
                this,
                ID,
                STORE_CODE,
                STORE_TYPE,
                FIELD_NAME,
                FIELD_VALUE,
                FIELD_DESC,
                CREATOR,
                MODIFIER
            )
                .values(
                    id,
                    storeCode,
                    storeType,
                    fieldName,
                    fieldValue,
                    fieldDesc,
                    userId,
                    userId
                ).execute()
        }
    }

    /**
     * 更新敏感数据
     */
    fun update(
        dslContext: DSLContext,
        userId: String,
        id: String,
        fieldName: String,
        fieldValue: String?,
        fieldDesc: String?
    ) {
        with(TStoreSensitiveConf.T_STORE_SENSITIVE_CONF) {
            val baseStep = dslContext.update(this)
                .set(FIELD_NAME, fieldName)

            if (fieldValue != null) baseStep.set(FIELD_VALUE, fieldValue)
            if (fieldDesc != null) baseStep.set(FIELD_DESC, fieldDesc)

            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(id))
                .execute()
        }
    }

    /**
     * 删除
     */
    fun batchDelete(
        dslContext: DSLContext,
        storeType: Byte,
        storeCode: String,
        idList: List<String>
    ) {
        with(TStoreSensitiveConf.T_STORE_SENSITIVE_CONF) {
            dslContext.deleteFrom(this)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType))
                .and(ID.`in`(idList))
                .execute()
        }
    }

    /**
     * 根据ID获取
     */
    fun getById(
        dslContext: DSLContext,
        id: String
    ): TStoreSensitiveConfRecord? {
        with(TStoreSensitiveConf.T_STORE_SENSITIVE_CONF) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .limit(1)
                .fetchOne()
        }
    }

    /**
     * 列表
     */
    fun list(
        dslContext: DSLContext,
        storeType: Byte,
        storeCode: String
    ): Result<TStoreSensitiveConfRecord>? {
        with(TStoreSensitiveConf.T_STORE_SENSITIVE_CONF) {
            return dslContext.selectFrom(this)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType))
                .fetch()
        }
    }
}