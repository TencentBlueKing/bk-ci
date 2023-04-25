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

package com.tencent.devops.prebuild.dao

import com.tencent.devops.model.prebuild.tables.TPrebuildPluginVersion
import com.tencent.devops.model.prebuild.tables.records.TPrebuildPluginVersionRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PreBuildPluginVersionDao {

    fun getVersion(
        pluginType: String,
        dslContext: DSLContext
    ): TPrebuildPluginVersionRecord? {
        with(TPrebuildPluginVersion.T_PREBUILD_PLUGIN_VERSION) {
            return dslContext.selectFrom(this)
                .where(PLUGIN_TYPE.eq(pluginType))
                .fetchAny()
        }
    }

    fun create(
        version: String,
        modifyUser: String,
        desc: String,
        pluginType: String,
        dslContext: DSLContext
    ): Int {
        with(TPrebuildPluginVersion.T_PREBUILD_PLUGIN_VERSION) {
            return dslContext.insertInto(
                this,
                VERSION,
                UPDATE_TIME,
                MODIFY_USER,
                DESC,
                PLUGIN_TYPE
            ).values(
                version,
                LocalDateTime.now(),
                modifyUser,
                desc,
                pluginType
            ).execute()
        }
    }

    fun update(
        version: String,
        modifyUser: String,
        desc: String,
        pluginType: String,
        dslContext: DSLContext
    ): Int {
        with(TPrebuildPluginVersion.T_PREBUILD_PLUGIN_VERSION) {
            return dslContext.update(this)
                .set(VERSION, version)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFY_USER, modifyUser)
                .set(DESC, desc)
                .where(PLUGIN_TYPE.eq(pluginType))
                .execute()
        }
    }

    fun delete(
        version: String,
        dslContext: DSLContext
    ): Int {
        with(TPrebuildPluginVersion.T_PREBUILD_PLUGIN_VERSION) {
            return dslContext.deleteFrom(this)
                .where(VERSION.eq(version))
                .execute()
        }
    }

    fun list(
        dslContext: DSLContext
    ): List<TPrebuildPluginVersionRecord>? {
        with(TPrebuildPluginVersion.T_PREBUILD_PLUGIN_VERSION) {
            return dslContext.selectFrom(this)
                .fetch()
        }
    }
}
