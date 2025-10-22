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

package com.tencent.devops.project.dao.leaf

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.leaf.segment.dao.IDAllocDao
import com.tencent.devops.leaf.segment.model.LeafAlloc
import com.tencent.devops.model.project.tables.TLeafAlloc
import com.tencent.devops.model.project.tables.records.TLeafAllocRecord
import javassist.NotFoundException
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class JooqIDAllocDaoImpl @Autowired constructor(
    private val dslContext: DSLContext
) : IDAllocDao {

    override fun getAllLeafAllocs(): MutableList<LeafAlloc> {
        with(TLeafAlloc.T_LEAF_ALLOC) {
            val leafAllocRecords = dslContext.selectFrom(this).skipCheck().fetch()
            val leafAllocs = mutableListOf<LeafAlloc>()
            leafAllocRecords.forEach { leafAllocRecord ->
                val leafAlloc = generateLeafAlloc(leafAllocRecord)
                leafAllocs.add(leafAlloc)
            }
            return leafAllocs
        }
    }

    private fun generateLeafAlloc(leafAllocRecord: TLeafAllocRecord?): LeafAlloc {
        val leafAlloc = LeafAlloc()
        leafAlloc.key = leafAllocRecord?.bizTag
        leafAlloc.maxId = leafAllocRecord?.maxId ?: 0
        leafAlloc.step = leafAllocRecord?.step ?: 0
        leafAlloc.updateTime = DateTimeUtil.toDateTime(leafAllocRecord?.updateTime)
        return leafAlloc
    }

    override fun updateMaxIdAndGetLeafAlloc(tag: String): LeafAlloc {
        with(TLeafAlloc.T_LEAF_ALLOC) {
            var leafAllocRecord: TLeafAllocRecord? = null
            dslContext.transaction { t ->
                val context = DSL.using(t)
                context.update(this)
                    .set(MAX_ID, MAX_ID + STEP)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .where(BIZ_TAG.eq(tag))
                    .execute()
                leafAllocRecord = context.selectFrom(this).where(BIZ_TAG.eq(tag)).fetchOne()
                    ?: throw NotFoundException("invalid tag")
            }
            return generateLeafAlloc(leafAllocRecord)
        }
    }

    override fun updateMaxIdByCustomStepAndGetLeafAlloc(leafAlloc: LeafAlloc): LeafAlloc {
        with(TLeafAlloc.T_LEAF_ALLOC) {
            var leafAllocRecord: TLeafAllocRecord? = null
            dslContext.transaction { t ->
                val context = DSL.using(t)
                context.update(this)
                    .set(MAX_ID, MAX_ID + leafAlloc.step)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .where(BIZ_TAG.eq(leafAlloc.key))
                    .execute()
                leafAllocRecord = context.selectFrom(this).where(BIZ_TAG.eq(leafAlloc.key)).fetchOne()
                    ?: throw NotFoundException("invalid tag")
            }
            return generateLeafAlloc(leafAllocRecord)
        }
    }

    override fun getAllTags(): MutableList<String> {
        with(TLeafAlloc.T_LEAF_ALLOC) {
            val tagRecords = dslContext.select(BIZ_TAG).from(this).skipCheck().fetch()
            return tagRecords.map { it.value1() }
        }
    }
}
