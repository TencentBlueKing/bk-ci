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

package com.tencent.devops.process.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.process.tables.TPipelineAtomReplaceItem
import com.tencent.devops.model.process.tables.records.TPipelineAtomReplaceItemRecord
import com.tencent.devops.store.pojo.atom.AtomVersionReplaceInfo
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class PipelineAtomReplaceItemDao {

    fun createAtomReplaceItem(
        dslContext: DSLContext,
        baseId: String,
        fromAtomCode: String,
        toAtomCode: String,
        versionInfoList: List<AtomVersionReplaceInfo>,
        userId: String
    ) {
        with(TPipelineAtomReplaceItem.T_PIPELINE_ATOM_REPLACE_ITEM) {
            versionInfoList.map {
                val paramReplaceInfoList = it.paramReplaceInfoList
                dslContext.insertInto(
                    this,
                    ID,
                    FROM_ATOM_CODE,
                    FROM_ATOM_VERSION,
                    TO_ATOM_CODE,
                    TO_ATOM_VERSION,
                    PARAM_REPLACE_INFO,
                    BASE_ID,
                    CREATOR,
                    MODIFIER
                )
                    .values(
                        UUIDUtil.generate(),
                        fromAtomCode,
                        it.fromAtomVersion,
                        toAtomCode,
                        it.toAtomVersion,
                        paramReplaceInfoList?.let { self -> JsonUtil.toJson(self, formatted = false) },
                        baseId,
                        userId,
                        userId
                    ).execute()
            }
        }
    }

    fun getAtomReplaceItemListByBaseId(
        dslContext: DSLContext,
        baseId: String,
        statusList: List<String>? = null,
        descFlag: Boolean,
        page: Int,
        pageSize: Int
    ): Result<TPipelineAtomReplaceItemRecord>? {
        with(TPipelineAtomReplaceItem.T_PIPELINE_ATOM_REPLACE_ITEM) {
            val conditions = getAtomReplaceItemListCondition(baseId, statusList)
            val baseStep = dslContext.selectFrom(this).where(conditions)
            if (descFlag) {
                baseStep.orderBy(CREATE_TIME.desc())
            } else {
                baseStep.orderBy(CREATE_TIME.asc())
            }
            return baseStep.limit((page - 1) * pageSize, pageSize).fetch()
        }
    }

    private fun TPipelineAtomReplaceItem.getAtomReplaceItemListCondition(
        baseId: String,
        statusList: List<String>?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(BASE_ID.eq(baseId))
        if (statusList != null) {
            conditions.add(STATUS.`in`(statusList))
        }
        return conditions
    }

    fun getAtomReplaceItemCountByBaseId(
        dslContext: DSLContext,
        baseId: String,
        statusList: List<String>? = null
    ): Long {
        with(TPipelineAtomReplaceItem.T_PIPELINE_ATOM_REPLACE_ITEM) {
            val conditions = getAtomReplaceItemListCondition(baseId, statusList)
            return dslContext.selectCount().from(this).where(conditions).fetchOne(0, Long::class.java)!!
        }
    }

    fun getAtomReplaceItem(
        dslContext: DSLContext,
        itemId: String
    ): TPipelineAtomReplaceItemRecord? {
        with(TPipelineAtomReplaceItem.T_PIPELINE_ATOM_REPLACE_ITEM) {
            return dslContext.selectFrom(this).where(ID.eq(itemId)).fetchOne()
        }
    }

    fun deleteByBaseId(dslContext: DSLContext, baseId: String) {
        with(TPipelineAtomReplaceItem.T_PIPELINE_ATOM_REPLACE_ITEM) {
            dslContext.deleteFrom(this)
                .where(BASE_ID.eq(baseId))
                .execute()
        }
    }

    fun updateAtomReplaceItemByBaseId(
        dslContext: DSLContext,
        baseId: String,
        status: String? = null,
        userId: String
    ) {
        with(TPipelineAtomReplaceItem.T_PIPELINE_ATOM_REPLACE_ITEM) {
            val baseStep = dslContext.update(this)
            if (status != null) {
                baseStep.set(STATUS, status)
            }
            baseStep.set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFIER, userId)
                .where(BASE_ID.eq(baseId))
                .execute()
        }
    }

    fun updateAtomReplaceItemByItemId(
        dslContext: DSLContext,
        itemId: String,
        status: String? = null,
        userId: String
    ) {
        with(TPipelineAtomReplaceItem.T_PIPELINE_ATOM_REPLACE_ITEM) {
            val baseStep = dslContext.update(this)
            if (status != null) {
                baseStep.set(STATUS, status)
            }
            baseStep.set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFIER, userId)
                .where(ID.eq(itemId))
                .execute()
        }
    }
}
