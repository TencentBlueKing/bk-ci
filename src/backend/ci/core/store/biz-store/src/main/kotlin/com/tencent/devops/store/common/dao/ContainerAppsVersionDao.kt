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

package com.tencent.devops.store.common.dao

import com.tencent.devops.model.store.tables.TAppVersion
import com.tencent.devops.model.store.tables.records.TAppVersionRecord
import com.tencent.devops.store.pojo.app.ContainerAppVersion
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Suppress("ALL")
@Repository
class ContainerAppsVersionDao {

    fun get(dslContext: DSLContext, appId: Int, version: String?): TAppVersionRecord? {
        return with(TAppVersion.T_APP_VERSION) {
            dslContext.selectFrom(this)
                .where(APP_ID.eq(appId))
                .and(VERSION.eq(version))
                .fetchOne()
        }
    }

    fun getById(dslContext: DSLContext, id: Int): TAppVersionRecord? {
        with(TAppVersion.T_APP_VERSION) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun add(dslContext: DSLContext, appId: Int, version: String?) {
        with(TAppVersion.T_APP_VERSION) {
            dslContext.insertInto(
                this,
                APP_ID,
                VERSION
            )
                .values(appId, version)
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, id: Int) {
        with(TAppVersion.T_APP_VERSION) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun deleteByAppId(dslContext: DSLContext, appId: Int) {
        with(TAppVersion.T_APP_VERSION) {
            dslContext.deleteFrom(this)
                .where(APP_ID.eq(appId))
                .execute()
        }
    }

    fun listByAppId(dslContext: DSLContext, appId: Int): Result<TAppVersionRecord> {
        with(TAppVersion.T_APP_VERSION) {
            return dslContext.selectFrom(this)
                .where(APP_ID.eq(appId))
                .orderBy(ID.desc())
                .fetch()
        }
    }

    fun update(dslContext: DSLContext, id: Int, appId: Int, version: String?) {
        with(TAppVersion.T_APP_VERSION) {
            dslContext.update(this)
                .set(APP_ID, appId)
                .set(VERSION, version)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun convert(record: TAppVersionRecord): ContainerAppVersion {
        with(record) {
            return ContainerAppVersion(id, appId, version)
        }
    }
}
