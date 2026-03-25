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
import com.tencent.devops.model.store.tables.TAtomLabelRel
import com.tencent.devops.model.store.tables.TLabel
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_ID
import com.tencent.devops.store.pojo.common.KEY_LABEL_CODE
import com.tencent.devops.store.pojo.common.KEY_LABEL_ID
import com.tencent.devops.store.pojo.common.KEY_LABEL_NAME
import com.tencent.devops.store.pojo.common.KEY_LABEL_TYPE
import com.tencent.devops.store.pojo.common.KEY_SERVICE_SCOPE
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import com.tencent.devops.store.pojo.common.enums.ServiceScopeEnum
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class AtomLabelRelDao {

    fun getLabelsByAtomIds(
        dslContext: DSLContext,
        atomIds: Set<String>,
        serviceScope: ServiceScopeEnum? = null
    ): Result<out Record>? {
        if (atomIds.isEmpty()) {
            return null
        }
        val tLabel = TLabel.T_LABEL
        val tAtomLabelRel = TAtomLabelRel.T_ATOM_LABEL_REL
        val conditions = mutableListOf<org.jooq.Condition>().apply {
            add(tAtomLabelRel.ATOM_ID.`in`(atomIds))
            // 如果提供了 serviceScope，添加 SERVICE_SCOPE 条件
            serviceScope?.let { add(tLabel.SERVICE_SCOPE.eq(it.name)) }
        }
        return dslContext.select(
            tLabel.ID.`as`(KEY_LABEL_ID),
            tLabel.LABEL_CODE.`as`(KEY_LABEL_CODE),
            tLabel.LABEL_NAME.`as`(KEY_LABEL_NAME),
            tLabel.SERVICE_SCOPE.`as`(KEY_SERVICE_SCOPE),
            tLabel.TYPE.`as`(KEY_LABEL_TYPE),
            tLabel.CREATE_TIME.`as`(KEY_CREATE_TIME),
            tLabel.UPDATE_TIME.`as`(KEY_UPDATE_TIME),
            tAtomLabelRel.ATOM_ID.`as`(KEY_ID)
        ).from(tLabel).join(tAtomLabelRel).on(tLabel.ID.eq(tAtomLabelRel.LABEL_ID))
            .where(conditions)
            .fetch()
    }

    fun deleteByAtomId(dslContext: DSLContext, atomId: String) {
        with(TAtomLabelRel.T_ATOM_LABEL_REL) {
            dslContext.deleteFrom(this)
                .where(ATOM_ID.eq(atomId))
                .execute()
        }
    }

    fun deleteByAtomCode(dslContext: DSLContext, atomCode: String) {
        val ta = TAtom.T_ATOM
        val atomIds = dslContext.select(ta.ID).from(ta).where(ta.ATOM_CODE.eq(atomCode)).fetch()
        with(TAtomLabelRel.T_ATOM_LABEL_REL) {
            dslContext.deleteFrom(this)
                .where(ATOM_ID.`in`(atomIds))
                .execute()
        }
    }

    fun batchAdd(dslContext: DSLContext, userId: String, atomId: String, labelIdList: List<String>) {
        with(TAtomLabelRel.T_ATOM_LABEL_REL) {
            val addStep = labelIdList.map {
                dslContext.insertInto(
                    this,
                    ID,
                    ATOM_ID,
                    LABEL_ID,
                    CREATOR,
                    MODIFIER
                )
                    .values(
                        UUIDUtil.generate(),
                        atomId,
                        it,
                        userId,
                        userId
                    )
            }
            dslContext.batch(addStep).execute()
        }
    }
}
