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

package com.tencent.devops.process.dao.op

import com.tencent.devops.model.process.tables.TPipelineGitciAtom
import com.tencent.devops.model.process.tables.records.TPipelineGitciAtomRecord
import com.tencent.devops.process.pojo.op.GitCiMarketAtomReq
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class GitCiMarketAtomDao {

    fun batchAdd(
        dslContext: DSLContext,
        userId: String,
        gitCiMarketAtomReq: GitCiMarketAtomReq
    ) {
        with(TPipelineGitciAtom.T_PIPELINE_GITCI_ATOM) {
            gitCiMarketAtomReq.atomCodeList.forEach {
                dslContext.insertInto(
                    this,
                    ATOM_CODE,
                    DESC,
                    UPDATE_TIME,
                    MODIFY_USER
                ).values(
                    it,
                    gitCiMarketAtomReq.desc,
                    LocalDateTime.now(),
                    userId
                )
                    .onDuplicateKeyUpdate()
                    .set(DESC, gitCiMarketAtomReq.desc)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .set(MODIFY_USER, userId)
                    .execute()
            }
        }
    }

    fun list(
        dslContext: DSLContext,
        atomCode: String?,
        page: Int?,
        pageSize: Int?
    ): List<TPipelineGitciAtomRecord> {
        with(TPipelineGitciAtom.T_PIPELINE_GITCI_ATOM) {
            val conditions = mutableListOf<Condition>()
            if (null != atomCode && atomCode.isNotBlank()) {
                conditions.add(ATOM_CODE.eq(atomCode))
            }
            val baseStep = dslContext.selectFrom(this).where(conditions)
            return if (null != page && null != pageSize) {
                baseStep.limit((page - 1) * pageSize, pageSize).fetch()
            } else {
                baseStep.fetch()
            }
        }
    }

    fun getCount(
        dslContext: DSLContext,
        atomCode: String?
    ): Long {
        with(TPipelineGitciAtom.T_PIPELINE_GITCI_ATOM) {
            val conditions = mutableListOf<Condition>()
            if (null != atomCode && atomCode.isNotBlank()) {
                conditions.add(ATOM_CODE.eq(atomCode))
            }
            return dslContext.selectCount()
                .from(this)
                .where(conditions)
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun delete(
        dslContext: DSLContext,
        atomCode: String
    ): Int {
        with(TPipelineGitciAtom.T_PIPELINE_GITCI_ATOM) {
            return dslContext.deleteFrom(this)
                .where(ATOM_CODE.eq(atomCode))
                .execute()
        }
    }
}
