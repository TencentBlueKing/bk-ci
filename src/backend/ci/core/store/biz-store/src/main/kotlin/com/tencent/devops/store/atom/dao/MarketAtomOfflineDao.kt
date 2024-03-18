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

package com.tencent.devops.store.atom.dao

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TAtomOffline
import com.tencent.devops.model.store.tables.records.TAtomOfflineRecord
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class MarketAtomOfflineDao {

    /**
     * 创建下线记录
     */
    fun create(dslContext: DSLContext, atomCode: String, bufferDay: Byte, userId: String, status: Byte) {
        val expireTime = LocalDateTime.now().plusDays(bufferDay.toLong())
        with(TAtomOffline.T_ATOM_OFFLINE) {
            dslContext.insertInto(
                this,
                ID,
                ATOM_CODE,
                BUFFER_DAY,
                EXPIRE_TIME,
                STATUS,
                CREATOR,
                MODIFIER
            )
                .values(
                    UUIDUtil.generate(),
                    atomCode,
                    bufferDay,
                    expireTime,
                    status,
                    userId,
                    userId
                ).execute()
        }
    }

    /**
     * 获取到期下线的记录
     */
    fun getExpiredAtoms(dslContext: DSLContext): Result<TAtomOfflineRecord> {
        with(TAtomOffline.T_ATOM_OFFLINE) {
            return dslContext.selectFrom(this)
                .where(EXPIRE_TIME.le(LocalDateTime.now()))
                .and(STATUS.eq(0))
                .fetch()
        }
    }

    /**
     * 设置状态
     */
    fun setStatus(dslContext: DSLContext, id: String, status: Byte, userId: String) {
        with(TAtomOffline.T_ATOM_OFFLINE) {
            dslContext.update(this)
                .set(STATUS, status)
                .set(MODIFIER, userId)
                .where(ID.eq(id))
                .execute()
        }
    }

    /**
     * 删除插件下线记录
     */
    fun deleteAtomOffline(dslContext: DSLContext, atomCode: String) {
        with(TAtomOffline.T_ATOM_OFFLINE) {
            dslContext.deleteFrom(this)
                .where(ATOM_CODE.eq(atomCode))
                .execute()
        }
    }
}
