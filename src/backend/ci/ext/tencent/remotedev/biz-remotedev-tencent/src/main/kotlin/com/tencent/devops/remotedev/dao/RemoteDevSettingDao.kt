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

package com.tencent.devops.remotedev.dao

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.service.utils.ByteUtils
import com.tencent.devops.model.remotedev.tables.TRemoteDevSettings
import com.tencent.devops.remotedev.pojo.RemoteDevSettings
import org.jooq.DSLContext
import org.jooq.Record2
import org.springframework.stereotype.Repository

@Repository
class RemoteDevSettingDao {

    fun createOrUpdateSetting(
        dslContext: DSLContext,
        setting: RemoteDevSettings,
        userId: String
    ) {
        with(TRemoteDevSettings.T_REMOTE_DEV_SETTINGS) {
            dslContext.insertInto(
                this,
                USER_ID,
                DEFAULT_SHELL,
                BASIC_SETTING,
                GIT_ATTACHED,
                TAPD_ATTACHED,
                GITHUB_ATTACHED,
                ENVS_FOR_VARIABLE
            )
                .values(
                    userId,
                    setting.defaultShell,
                    JsonUtil.toJson(setting.basicSetting, false),
                    ByteUtils.bool2Byte(setting.gitAttached),
                    ByteUtils.bool2Byte(setting.tapdAttached),
                    ByteUtils.bool2Byte(setting.githubAttached),
                    JsonUtil.toJson(setting.envsForVariable, false)
                ).onDuplicateKeyUpdate()
                .set(DEFAULT_SHELL, setting.defaultShell)
                .set(BASIC_SETTING, JsonUtil.toJson(setting.basicSetting, false))
                .set(GIT_ATTACHED, ByteUtils.bool2Byte(setting.gitAttached))
                .set(TAPD_ATTACHED, ByteUtils.bool2Byte(setting.tapdAttached))
                .set(GITHUB_ATTACHED, ByteUtils.bool2Byte(setting.githubAttached))
                .set(ENVS_FOR_VARIABLE, JsonUtil.toJson(setting.envsForVariable, false))
                .execute()
        }
    }

    fun fetchAnySetting(
        dslContext: DSLContext,
        userId: String
    ): RemoteDevSettings? {
        return with(TRemoteDevSettings.T_REMOTE_DEV_SETTINGS) {
            dslContext.selectFrom(this).where(USER_ID.eq(userId)).fetchAny()?.let {
                RemoteDevSettings(
                    defaultShell = it.defaultShell,
                    basicSetting = JsonUtil.toOrNull(
                        it.basicSetting,
                        object : TypeReference<Map<String, String>>() {}
                    ) ?: emptyMap(),
                    gitAttached = ByteUtils.byte2Bool(it.gitAttached),
                    tapdAttached = ByteUtils.byte2Bool(it.tapdAttached),
                    githubAttached = ByteUtils.byte2Bool(it.githubAttached),
                    envsForVariable = JsonUtil.toOrNull(
                        it.envsForVariable,
                        object : TypeReference<Map<String, String>>() {}
                    ) ?: emptyMap(),
                    envsForFile = emptyList()
                )
            }
        }
    }

    fun fetchSingleUserBilling(
        dslContext: DSLContext,
        userId: String
    ): Record2<Int, Int> {
        return with(TRemoteDevSettings.T_REMOTE_DEV_SETTINGS) {
            dslContext.select(CUMULATIVE_USAGE_TIME, CUMULATIVE_BILLING_TIME).from(this)
                .where(USER_ID.eq(userId))
                .fetchSingle()
        }
    }
}
