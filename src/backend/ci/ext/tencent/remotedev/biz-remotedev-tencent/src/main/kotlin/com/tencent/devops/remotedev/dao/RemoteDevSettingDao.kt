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
import com.tencent.devops.remotedev.pojo.OPUserSetting
import com.tencent.devops.remotedev.pojo.RemoteDevSettings
import com.tencent.devops.remotedev.pojo.RemoteDevUserSettings
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

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
                TAPD_ATTACHED,
                ENVS_FOR_VARIABLE,
                DOTFILE_REPO,
                USER_SETTING
            )
                .values(
                    userId,
                    setting.defaultShell,
                    JsonUtil.toJson(setting.basicSetting, false),
                    ByteUtils.bool2Byte(setting.tapdAttached),
                    JsonUtil.toJson(setting.envsForVariable, false),
                    setting.dotfileRepo,
                    setting.userSetting.let { JsonUtil.toJson(it, false) }

                ).onDuplicateKeyUpdate()
                .set(DEFAULT_SHELL, setting.defaultShell)
                .set(BASIC_SETTING, JsonUtil.toJson(setting.basicSetting, false))
                .set(TAPD_ATTACHED, ByteUtils.bool2Byte(setting.tapdAttached))
                .set(ENVS_FOR_VARIABLE, JsonUtil.toJson(setting.envsForVariable, false))
                .set(DOTFILE_REPO, setting.dotfileRepo)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(USER_SETTING, setting.userSetting.let { JsonUtil.toJson(it, false) })
                .execute()
        }
    }

    fun fetchAnySetting(
        dslContext: DSLContext,
        userId: String
    ): RemoteDevSettings {
        return with(TRemoteDevSettings.T_REMOTE_DEV_SETTINGS) {
            dslContext.selectFrom(this).where(USER_ID.eq(userId)).fetchAny()?.let {
                RemoteDevSettings(
                    defaultShell = it.defaultShell,
                    basicSetting = JsonUtil.toOrNull(
                        it.basicSetting,
                        object : TypeReference<Map<String, String>>() {}
                    ) ?: emptyMap(),
                    gitAttached = false,
                    tapdAttached = ByteUtils.byte2Bool(it.tapdAttached),
                    githubAttached = false,
                    envsForVariable = JsonUtil.toOrNull(
                        it.envsForVariable,
                        object : TypeReference<Map<String, String>>() {}
                    ) ?: emptyMap(),
                    envsForFile = emptyList(),
                    dotfileRepo = it.dotfileRepo,
                    projectId = it.projectId ?: "",
                    userSetting = JsonUtil.toOrNull(
                        it.userSetting, RemoteDevUserSettings::class.java
                    ) ?: RemoteDevUserSettings()
                )
            } ?: run {
                createOrUpdateSetting(dslContext, RemoteDevSettings(), userId)
                return RemoteDevSettings()
            }
        }
    }

    fun updateProjectId(
        dslContext: DSLContext,
        userId: String,
        projectId: String
    ): Boolean {
        with(TRemoteDevSettings.T_REMOTE_DEV_SETTINGS) {
            return dslContext.update(this).set(PROJECT_ID, projectId).where(USER_ID.eq(userId)).execute() == 1
        }
    }

    fun fetchAnyOpUserSetting(
        dslContext: DSLContext,
        userId: String
    ): OPUserSetting? {
        return with(TRemoteDevSettings.T_REMOTE_DEV_SETTINGS) {
            dslContext.selectFrom(this).where(USER_ID.eq(userId)).fetchAny()?.let {
                OPUserSetting(
                    userId = it.userId,
                    wsMaxRunningCount = it.workspaceMaxRunningCount,
                    wsMaxHavingCount = it.workspaceMaxHavingCount,
                    grayFlag = ByteUtils.byte2Bool(it.inGray),
                    onlyCloudIDE = JsonUtil.toOrNull(it.userSetting, RemoteDevUserSettings::class.java)?.onlyCloudIDE
                )
            }
        }
    }

    fun fetchSingleUserBilling(
        dslContext: DSLContext,
        userId: String
    ): Pair<Int, Int> {
        return with(TRemoteDevSettings.T_REMOTE_DEV_SETTINGS) {
            dslContext.select(CUMULATIVE_USAGE_TIME, CUMULATIVE_BILLING_TIME).from(this)
                .where(USER_ID.eq(userId))
                .fetchAny()?.let { it.value1() to it.value2() } ?: run {
                createOrUpdateSetting(dslContext, RemoteDevSettings(), userId)
                return 0 to 0
            }
        }
    }

    fun fetchSingleUserWsCount(
        dslContext: DSLContext,
        userId: String
    ): Pair<Int?, Int?> {
        return with(TRemoteDevSettings.T_REMOTE_DEV_SETTINGS) {
            dslContext.select(WORKSPACE_MAX_RUNNING_COUNT, WORKSPACE_MAX_HAVING_COUNT).from(this)
                .where(USER_ID.eq(userId))
                .fetchAny()?.let { it.value1() to it.value2() } ?: (null to null)
        }
    }

    fun createOrUpdateSetting4OP(
        dslContext: DSLContext,
        opSetting: OPUserSetting
    ) {
        val setting = RemoteDevSettings()
        val userSetting = RemoteDevUserSettings()
        with(TRemoteDevSettings.T_REMOTE_DEV_SETTINGS) {
            dslContext.insertInto(
                this,
                USER_ID,
                DEFAULT_SHELL,
                BASIC_SETTING,
                TAPD_ATTACHED,
                ENVS_FOR_VARIABLE,
                DOTFILE_REPO,
                WORKSPACE_MAX_RUNNING_COUNT,
                WORKSPACE_MAX_HAVING_COUNT,
                IN_GRAY
            )
                .values(
                    opSetting.userId,
                    setting.defaultShell,
                    JsonUtil.toJson(setting.basicSetting, false),
                    ByteUtils.bool2Byte(setting.tapdAttached),
                    JsonUtil.toJson(setting.envsForVariable, false),
                    setting.dotfileRepo,
                    opSetting.wsMaxRunningCount,
                    opSetting.wsMaxHavingCount,
                    ByteUtils.bool2Byte(opSetting.grayFlag ?: false)
                ).onDuplicateKeyUpdate()
                .set(UPDATE_TIME, LocalDateTime.now())
                .let {
                    if (opSetting.wsMaxRunningCount != null) {
                        userSetting.maxRunningCount = opSetting.wsMaxRunningCount!!
                        it.set(
                            WORKSPACE_MAX_RUNNING_COUNT,
                            opSetting.wsMaxRunningCount
                        )
                    } else it
                }
                .let {
                    if (opSetting.wsMaxHavingCount != null) {
                        userSetting.maxHavingCount = opSetting.wsMaxHavingCount!!
                        it.set(
                            WORKSPACE_MAX_HAVING_COUNT,
                            opSetting.wsMaxHavingCount
                        )
                    } else it
                }
                .let {
                    if (opSetting.grayFlag != null) it.set(
                        IN_GRAY,
                        ByteUtils.bool2Byte(opSetting.grayFlag!!)
                    ) else it
                }
                .let {
                    if (opSetting.onlyCloudIDE != null) {
                        userSetting.onlyCloudIDE = opSetting.onlyCloudIDE!!
                        it.set(
                            USER_SETTING,
                            JsonUtil.toJson(userSetting, false)
                        )
                    } else it
                }
                .execute()
        }
    }
}
