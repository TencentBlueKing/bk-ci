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

package com.tencent.devops.store.dao.atom

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TAtomLabelRel
import com.tencent.devops.model.store.tables.TLabel
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class AtomLabelRelDao {

    fun getLabelsByAtomId(
        dslContext: DSLContext,
        atomId: String
    ): Result<out Record>? {
        val a = TLabel.T_LABEL.`as`("a")
        val b = TAtomLabelRel.T_ATOM_LABEL_REL.`as`("b")
        return dslContext.select(
            a.ID.`as`("id"),
            a.LABEL_CODE.`as`("labelCode"),
            a.LABEL_NAME.`as`("labelName"),
            a.TYPE.`as`("labelType"),
            a.CREATE_TIME.`as`("createTime"),
            a.UPDATE_TIME.`as`("updateTime")
        ).from(a).join(b).on(a.ID.eq(b.LABEL_ID))
            .where(b.ATOM_ID.eq(atomId))
            .fetch()
    }

    fun deleteByAtomId(dslContext: DSLContext, atomId: String) {
        with(TAtomLabelRel.T_ATOM_LABEL_REL) {
            dslContext.deleteFrom(this)
                .where(ATOM_ID.eq(atomId))
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