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

import com.tencent.devops.model.store.tables.TAtomEnvInfo
import com.tencent.devops.model.store.tables.TStoreBuildInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record3
import org.springframework.stereotype.Repository

@Repository
class MarketAtomBuildInfoDao {

    fun getAtomBuildInfo(dslContext: DSLContext, atomId: String): Record3<String, String, String> {
        val tAtomEnvInfo = TAtomEnvInfo.T_ATOM_ENV_INFO
        val tStoreBuildInfo = TStoreBuildInfo.T_STORE_BUILD_INFO
        val conditions = mutableListOf<Condition>()
        conditions.add(tAtomEnvInfo.ATOM_ID.eq(atomId))
        conditions.add(tAtomEnvInfo.DEFAULT_FLAG.eq(true))
        conditions.add(tStoreBuildInfo.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))
        return dslContext.select(
            tStoreBuildInfo.SCRIPT,
            tStoreBuildInfo.REPOSITORY_PATH,
            tStoreBuildInfo.LANGUAGE
        ).from(tAtomEnvInfo)
            .join(tStoreBuildInfo)
            .on(tAtomEnvInfo.LANGUAGE.eq(tStoreBuildInfo.LANGUAGE))
            .where(conditions)
            .limit(1)
            .fetchOne()!!
    }
}
