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

package com.tencent.devops.store.dao.atom

import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_CODE
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_NAME
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_ID
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class MarketAtomClassifyDao : AtomBaseDao() {

    fun getAllAtomClassify(dslContext: DSLContext): Result<out Record>? {
        val tAtom = TAtom.T_ATOM
        val tClassify = TClassify.T_CLASSIFY
        val conditions = setAtomVisibleCondition(tAtom)
        conditions.add(0, tAtom.CLASSIFY_ID.eq(tClassify.ID))
        val atomNum = dslContext.selectCount().from(tAtom).where(conditions).asField<Int>("atomNum")
        return dslContext.select(
            tClassify.ID.`as`(KEY_ID),
            tClassify.CLASSIFY_CODE.`as`(KEY_CLASSIFY_CODE),
            tClassify.CLASSIFY_NAME.`as`(KEY_CLASSIFY_NAME),
            atomNum,
            tClassify.CREATE_TIME.`as`(KEY_CREATE_TIME),
            tClassify.UPDATE_TIME.`as`(KEY_UPDATE_TIME)
        ).from(tClassify).where(tClassify.TYPE.eq(0)).orderBy(tClassify.WEIGHT.desc()).fetch()
    }
}
