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

import com.tencent.devops.model.store.tables.TAppVersion
import com.tencent.devops.model.store.tables.TApps
import com.tencent.devops.model.store.tables.TAtomBuildAppRel
import com.tencent.devops.model.store.tables.TAtomBuildInfo
import com.tencent.devops.model.store.tables.TAtomEnvInfo
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class MarketAtomBuildAppRelDao {

    fun getMarketAtomBuildAppInfo(dslContext: DSLContext, atomId: String): Result<out Record>? {
        val a = TAtomBuildInfo.T_ATOM_BUILD_INFO.`as`("a")
        val b = TAtomBuildAppRel.T_ATOM_BUILD_APP_REL.`as`("b")
        val c = TAppVersion.T_APP_VERSION.`as`("c")
        val d = TApps.T_APPS.`as`("d")
        val e = TAtomEnvInfo.T_ATOM_ENV_INFO.`as`("e")
        return dslContext.select(
            d.NAME.`as`("appName"),
            c.VERSION.`as`("appVersion")
        ).from(a)
            .join(b)
            .on(a.ID.eq(b.BUILD_INFO_ID))
            .join(c)
            .on(b.APP_VERSION_ID.eq(c.ID))
            .join(d)
            .on(c.APP_ID.eq(d.ID))
            .join(e)
            .on(a.LANGUAGE.eq(e.LANGUAGE))
            .where(e.ATOM_ID.eq(atomId))
            .fetch()
    }
}