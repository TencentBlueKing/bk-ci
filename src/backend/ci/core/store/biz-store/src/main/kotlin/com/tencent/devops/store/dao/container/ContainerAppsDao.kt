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

package com.tencent.devops.store.dao.container

import com.tencent.devops.model.store.tables.TApps
import com.tencent.devops.model.store.tables.records.TAppsRecord
import com.tencent.devops.store.pojo.app.ContainerApp
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class ContainerAppsDao {

    fun exist(dslContext: DSLContext, id: Int): Boolean {
        with(TApps.T_APPS) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne() != null
        }
    }

    fun get(dslContext: DSLContext, name: String, os: String): TAppsRecord? {
        return with(TApps.T_APPS) {
            dslContext.selectFrom(this)
                .where(NAME.eq(name))
                .and(OS.eq(os))
                .fetchOne()
        }
    }

    fun getContainerAppInfo(dslContext: DSLContext, id: Int): TAppsRecord? {
        return with(TApps.T_APPS) {
            dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun getAllContainerApps(dslContext: DSLContext): Result<TAppsRecord> {
        with(TApps.T_APPS) {
            return dslContext
                .selectFrom(this)
                .orderBy(ID.desc())
                .fetch()
        }
    }

    fun add(dslContext: DSLContext, name: String, os: String, binPath: String?): Int {
        return with(TApps.T_APPS) {
            dslContext.insertInto(
                this,
                NAME,
                OS,
                BIN_PATH
            )
                .values(name, os, binPath)
                .returning(ID)
                .fetchOne().id
        }
    }

    fun countByNameAndOs(dslContext: DSLContext, name: String, os: String): Int {
        with(TApps.T_APPS) {
            return dslContext.selectCount().from(this).where(NAME.eq(name).and(OS.eq(os))).fetchOne(0, Int::class.java)
        }
    }

    fun delete(dslContext: DSLContext, id: Int) {
        with(TApps.T_APPS) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun listByOS(dslContext: DSLContext, os: String): Result<TAppsRecord> {
        with(TApps.T_APPS) {
            return dslContext.selectFrom(this)
                .where(OS.eq(os))
                .fetch()
        }
    }

    fun update(dslContext: DSLContext, id: Int, name: String, os: String, binPath: String?) {
        with(TApps.T_APPS) {
            dslContext.update(this)
                .set(NAME, name)
                .set(OS, os)
                .set(BIN_PATH, binPath)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun convert(record: TAppsRecord): ContainerApp {
        with(record) {
            return ContainerApp(id = id, name = name, os = os, binPath = binPath)
        }
    }
}