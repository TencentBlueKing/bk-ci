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

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TStoreMember
import com.tencent.devops.model.store.tables.records.TStoreMemberRecord
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class StoreMemberDao {

    fun addStoreMember(
        dslContext: DSLContext,
        userId: String,
        storeCode: String,
        userName: String,
        type: Byte,
        storeType: Byte
    ) {
        with(TStoreMember.T_STORE_MEMBER) {
            dslContext.insertInto(
                this,
                ID,
                STORE_CODE,
                USERNAME,
                TYPE,
                STORE_TYPE,
                CREATOR,
                MODIFIER
            )
                .values(
                    UUIDUtil.generate(),
                    storeCode,
                    userName,
                    type,
                    storeType,
                    userId,
                    userId
                ).execute()
        }
    }

    /**
     * 获取store组件成员列表
     */
    fun list(dslContext: DSLContext, storeCode: String, type: Byte?, storeType: Byte): Result<TStoreMemberRecord>? {
        with(TStoreMember.T_STORE_MEMBER) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_CODE.eq(storeCode))
            conditions.add(STORE_TYPE.eq(storeType))
            if (type != null) {
                conditions.add(TYPE.eq(type))
            }
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetch()
        }
    }

    /**
     * 获取store组件成员
     */
    fun getById(dslContext: DSLContext, id: String): TStoreMemberRecord? {
        with(TStoreMember.T_STORE_MEMBER) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    /**
     * 判断用户是否为store组件管理员
     */
    fun isStoreAdmin(dslContext: DSLContext, userId: String, storeCode: String, storeType: Byte): Boolean {
        with(TStoreMember.T_STORE_MEMBER) {
            return dslContext.selectCount()
                .from(this)
                .where(STORE_CODE.eq(storeCode))
                .and(USERNAME.eq(userId))
                .and(STORE_TYPE.eq(storeType))
                .and(TYPE.eq(StoreMemberTypeEnum.ADMIN.type.toByte()))
                .fetchOne(0, Long::class.java) != 0L
        }
    }

    /**
     * 判断用户是否为store组件成员
     */
    fun isStoreMember(dslContext: DSLContext, userId: String, storeCode: String, storeType: Byte): Boolean {
        with(TStoreMember.T_STORE_MEMBER) {
            return dslContext.selectCount()
                .from(this)
                .where(STORE_CODE.eq(storeCode))
                .and(USERNAME.eq(userId))
                .and(STORE_TYPE.eq(storeType))
                .fetchOne(0, Long::class.java) != 0L
        }
    }

    /**
     * 删除全部成员
     */
    fun deleteAll(dslContext: DSLContext, storeCode: String, storeType: Byte) {
        with(TStoreMember.T_STORE_MEMBER) {
            dslContext.deleteFrom(this)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType))
                .execute()
        }
    }

    /**
     * 删除成员
     */
    fun delete(dslContext: DSLContext, memberId: String) {
        with(TStoreMember.T_STORE_MEMBER) {
            dslContext.deleteFrom(this)
                .where(ID.eq(memberId))
                .execute()
        }
    }

    /**
     * 批量获取用户
     */
    fun batchList(dslContext: DSLContext, storeCodeList: List<String?>, storeType: Byte): Result<TStoreMemberRecord>? {
        with(TStoreMember.T_STORE_MEMBER) {
            return dslContext.selectFrom(this)
                .where(STORE_CODE.`in`(storeCodeList))
                .and(STORE_TYPE.eq(storeType))
                .fetch()
        }
    }

    /**
     * 获取管理员个数
     */

    fun countAdmin(dslContext: DSLContext, storeCode: String, storeType: Byte): Int {
        with(TStoreMember.T_STORE_MEMBER) {
            return dslContext.selectCount()
                .from(this)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType))
                .and(TYPE.eq(0))
                .fetchOne(0, Int::class.java)
        }
    }
}