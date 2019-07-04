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

import com.tencent.devops.model.store.tables.TAppEnv
import com.tencent.devops.model.store.tables.records.TAppEnvRecord
import com.tencent.devops.store.pojo.app.ContainerAppEnv
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class ContainerAppsEnvDao {

    fun add(dslContext: DSLContext, appId: Int, name: String, path: String, description: String) {
        with(TAppEnv.T_APP_ENV) {
            dslContext.insertInto(
                this,
                APP_ID,
                NAME,
                PATH,
                DESCRIPTION
            )
                .values(appId, name, path, description)
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, id: Int) {
        with(TAppEnv.T_APP_ENV) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun deleteByAppId(dslContext: DSLContext, appId: Int) {
        with(TAppEnv.T_APP_ENV) {
            dslContext.deleteFrom(this)
                .where(APP_ID.eq(appId))
                .execute()
        }
    }

    fun getById(dslContext: DSLContext, id: Int): TAppEnvRecord? {
        with(TAppEnv.T_APP_ENV) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun listByAppId(dslContext: DSLContext, appId: Int): Result<TAppEnvRecord> {
        with(TAppEnv.T_APP_ENV) {
            return dslContext.selectFrom(this)
                .where(APP_ID.eq(appId))
                .fetch()
        }
    }

    fun update(dslContext: DSLContext, id: Int, appId: Int, name: String, path: String, description: String) {
        with(TAppEnv.T_APP_ENV) {
            dslContext.update(this)
                .set(APP_ID, appId)
                .set(NAME, name)
                .set(PATH, path)
                .set(DESCRIPTION, description)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun convert(record: TAppEnvRecord): ContainerAppEnv {
        with(record) {
            return ContainerAppEnv(id = id, appId = appId, path = path, name = name, description = description)
        }
    }
}