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

package com.tencent.devops.store.atom.dao

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.TAtomVersionLog
import com.tencent.devops.model.store.tables.records.TAtomVersionLogRecord
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Suppress("ALL")
@Repository
class MarketAtomVersionLogDao {

    fun addMarketAtomVersion(
        dslContext: DSLContext,
        userId: String,
        atomId: String,
        releaseType: Byte,
        versionContent: String
    ) {
        with(TAtomVersionLog.T_ATOM_VERSION_LOG) {
            dslContext.insertInto(this,
                ID,
                ATOM_ID,
                RELEASE_TYPE,
                CONTENT,
                CREATOR,
                MODIFIER
            )
                .values(
                    UUIDUtil.generate(),
                    atomId,
                    releaseType,
                    versionContent,
                    userId,
                    userId
                )
                .onDuplicateKeyUpdate()
                .set(RELEASE_TYPE, releaseType)
                .set(CONTENT, versionContent)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun getAtomVersion(dslContext: DSLContext, atomId: String): TAtomVersionLogRecord {
        with(TAtomVersionLog.T_ATOM_VERSION_LOG) {
            return dslContext.selectFrom(this)
                .where(ATOM_ID.eq(atomId))
                .fetchOne()!!
        }
    }

    fun getAtomVersions(
        dslContext: DSLContext,
        atomIds: List<String>,
        getTestVersionFlag: Boolean = false
    ): Result<TAtomVersionLogRecord>? {
        with(TAtomVersionLog.T_ATOM_VERSION_LOG) {
            val step = dslContext.selectFrom(this)
                .where(ATOM_ID.`in`(atomIds))
            val conditionStep = if (!getTestVersionFlag) {
                step.and(RELEASE_TYPE.notEqual(ReleaseTypeEnum.BRANCH_TEST.releaseType.toByte()))
            } else { step }
            return conditionStep.fetch()
        }
    }

    fun deleteByAtomCode(dslContext: DSLContext, atomCode: String) {
        val ta = TAtom.T_ATOM
        val atomIds = dslContext.select(ta.ID).from(ta).where(ta.ATOM_CODE.eq(atomCode)).fetch()
        with(TAtomVersionLog.T_ATOM_VERSION_LOG) {
            dslContext.deleteFrom(this)
                .where(ATOM_ID.`in`(atomIds))
                .execute()
        }
    }
}
