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
import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.ci.UserUtil
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.common.service.utils.ByteUtils
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.model.remotedev.tables.TRemoteDevSettings
import com.tencent.devops.model.remotedev.tables.records.TRemoteDevSettingsRecord
import com.tencent.devops.remotedev.pojo.OPUserSetting
import com.tencent.devops.remotedev.pojo.RemoteDevSettings
import com.tencent.devops.remotedev.pojo.RemoteDevUserSettings
import com.tencent.devops.remotedev.service.client.TaiClient
import com.tencent.devops.remotedev.service.client.TaiUserInfoRequest
import java.time.LocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class RemoteDevSettingDao {

    fun createOrUpdateSetting(
        dslContext: DSLContext,
        setting: RemoteDevSettings,
        userId: String
    ) {
        val userInfo = userNameAndCompany(userId)
        with(TRemoteDevSettings.T_REMOTE_DEV_SETTINGS) {
            dslContext.insertInto(
                this,
                USER_ID,
                DEFAULT_SHELL,
                BASIC_SETTING,
                TAPD_ATTACHED,
                ENVS_FOR_VARIABLE,
                DOTFILE_REPO,
                USER_SETTING,
                USER_NAME,
                COMPANY_NAME
            )
                .values(
                    userId,
                    setting.defaultShell,
                    JsonUtil.toJson(setting.basicSetting, false),
                    ByteUtils.bool2Byte(setting.tapdAttached),
                    JsonUtil.toJson(setting.envsForVariable, false),
                    setting.dotfileRepo,
                    JsonUtil.toJson(RemoteDevUserSettings(), false),
                    userInfo.value?.accountName ?: "",
                    userInfo.value?.companyTags?.joinToString(",") { it.tagName } ?: ""
                ).onDuplicateKeyUpdate()
                .set(DEFAULT_SHELL, setting.defaultShell)
                .set(BASIC_SETTING, JsonUtil.toJson(setting.basicSetting, false))
                .set(TAPD_ATTACHED, ByteUtils.bool2Byte(setting.tapdAttached))
                .set(ENVS_FOR_VARIABLE, JsonUtil.toJson(setting.envsForVariable, false))
                .set(DOTFILE_REPO, setting.dotfileRepo)
                .set(UPDATE_TIME, LocalDateTime.now())
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
                    ) ?: RemoteDevUserSettings(),
                    userName = it.userName,
                    companyName = it.companyName
                )
            }
        }
    }

    fun fetchOneSetting(
        dslContext: DSLContext,
        userId: String
    ): RemoteDevSettings {
        return fetchAnySetting(dslContext, userId) ?: run {
            createOrUpdateSetting(dslContext, RemoteDevSettings(), userId)
            return RemoteDevSettings()
        }
    }

    fun fetchTaiUserInfo(
        dslContext: DSLContext,
        limit: SQLLimit? = null,
        userIds: Set<String>? = null
    ): Map<String, Pair<String, String>> {
        if (userIds?.isEmpty() == true) return emptyMap()
        return with(TRemoteDevSettings.T_REMOTE_DEV_SETTINGS) {
            dslContext.select(USER_ID, USER_NAME, COMPANY_NAME).from(this)
                .let {
                    if (userIds != null) {
                        it.where(USER_ID.`in`(userIds))
                    } else {
                        it.where(USER_ID.like("%@tai"))
                    }
                }
                .let {
                    if (limit != null) it.limit(limit.limit).offset(limit.offset) else it
                }
                .skipCheck()
                .fetch {
                    it.value1() to Pair(it.value2(), it.value3())
                }.toMap()
        }
    }

    fun updateTaiUserInfo(
        dslContext: DSLContext,
        userInfo: Map<String, Pair<String, String>>
    ) {
        with(TRemoteDevSettings.T_REMOTE_DEV_SETTINGS) {
            dslContext.batched { c ->
                userInfo.forEach { (t, u) ->
                    c.dsl().update(this).set(USER_NAME, u.first)
                        .set(COMPANY_NAME, u.second)
                        .where(USER_ID.eq(t))
                        .execute()
                }
            }
        }
    }

    fun fetchAllUserSettings(
        dslContext: DSLContext,
        queryUser: String?,
        limit: SQLLimit
    ): Result<TRemoteDevSettingsRecord> {
        with(TRemoteDevSettings.T_REMOTE_DEV_SETTINGS) {
            val condition = mutableListOf<Condition>()
            condition.add(USER_SETTING.ne(""))
            if (!queryUser.isNullOrBlank()) {
                condition.add(USER_ID.like("%$queryUser%"))
            }
            return dslContext.selectFrom(this)
                .where(condition)
                .limit(limit.limit).offset(limit.offset)
                .skipCheck()
                .fetch()
        }
    }

    fun countAllUserSettings(
        dslContext: DSLContext,
        queryUser: String?
    ): Long {
        with(TRemoteDevSettings.T_REMOTE_DEV_SETTINGS) {
            val condition = mutableListOf<Condition>()
            condition.add(USER_SETTING.ne(""))
            if (!queryUser.isNullOrBlank()) {
                condition.add(USER_ID.like("%$queryUser%"))
            }
            return dslContext.selectCount().from(this)
                .where(condition)
                .skipCheck()
                .fetchOne(0, Long::class.java)!!
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

    fun fetchSingleUserBilling(
        dslContext: DSLContext,
        userId: String
    ): Pair<Int, Int> {
        return with(TRemoteDevSettings.T_REMOTE_DEV_SETTINGS) {
            dslContext.select(CUMULATIVE_USAGE_TIME, CUMULATIVE_BILLING_TIME).from(this)
                .where(USER_ID.eq(userId))
                .fetchAny()?.let { it.value1() to it.value2() } ?: run {
                return 0 to 0
            }
        }
    }

    fun fetchAnyUserSetting(
        dslContext: DSLContext,
        userId: String
    ): RemoteDevUserSettings {
        return with(TRemoteDevSettings.T_REMOTE_DEV_SETTINGS) {
            dslContext.select(USER_SETTING).from(this)
                .where(USER_ID.eq(userId))
                .fetchAny()?.let { JsonUtil.toOrNull(it.value1(), RemoteDevUserSettings::class.java) } ?: run {
                createOrUpdateSetting4OP(dslContext, userId, null)
                return RemoteDevUserSettings()
            }
        }
    }

    fun batchUpdateWinUsageRemainingTime(
        dslContext: DSLContext,
        data: List<Pair<String, Int>>
    ) {
        with(TRemoteDevSettings.T_REMOTE_DEV_SETTINGS) {
            data.forEach { (userId, time) ->
                dslContext.update(this)
                    .set(WIN_USAGE_REMAINING_TIME, time).where(USER_ID.eq(userId)).execute()
            }
        }
    }

    fun fetchSingleUserWinTimeLeft(
        dslContext: DSLContext,
        userId: String
    ): Int? {
        with(TRemoteDevSettings.T_REMOTE_DEV_SETTINGS) {
            return dslContext.select(WIN_USAGE_REMAINING_TIME)
                .from(this).where(USER_ID.eq(userId)).fetchAny(WIN_USAGE_REMAINING_TIME)
        }
    }

    @Suppress("ComplexMethod")
    fun createOrUpdateSetting4OP(
        dslContext: DSLContext,
        userId: String,
        opSetting: OPUserSetting?
    ) {
        val setting = RemoteDevSettings()
        val userSetting = RemoteDevUserSettings().apply {
            maxRunningCount = opSetting?.maxRunningCount ?: maxRunningCount
            maxHavingCount = opSetting?.maxHavingCount ?: maxHavingCount
            onlyCloudIDE = opSetting?.onlyCloudIDE ?: onlyCloudIDE
            allowedCopy = opSetting?.allowedCopy ?: allowedCopy
            startCloudExperienceDuration = opSetting?.startCloudExperienceDuration ?: startCloudExperienceDuration
            allowedDownload = opSetting?.allowedDownload ?: allowedDownload
            needWatermark = opSetting?.needWatermark ?: needWatermark
            autoDeletedDays = opSetting?.autoDeletedDays ?: autoDeletedDays
            mountType = opSetting?.mountType ?: mountType
        }
        val userInfo = userNameAndCompany(userId)
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
                USER_SETTING,
                USER_NAME,
                COMPANY_NAME
            )
                .values(
                    userId,
                    setting.defaultShell,
                    JsonUtil.toJson(setting.basicSetting, false),
                    ByteUtils.bool2Byte(setting.tapdAttached),
                    JsonUtil.toJson(setting.envsForVariable, false),
                    setting.dotfileRepo,
                    userSetting.maxRunningCount,
                    userSetting.maxHavingCount,
                    JsonUtil.toJson(userSetting, false),
                    userInfo.value?.username ?: "",
                    userInfo.value?.companyTags?.joinToString(",") { it.tagName } ?: ""
                ).onDuplicateKeyUpdate()
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(
                    USER_SETTING,
                    JsonUtil.toJson(userSetting, false)
                )
                .execute()
        }
    }

    private fun userNameAndCompany(userId: String) = lazy {
        if (UserUtil.isTaiUser(userId)) {
            val taiClient = SpringContextUtil.getBean(TaiClient::class.java)
            taiClient.taiUserInfo(TaiUserInfoRequest(setOf(userId))).first()
        } else null
    }
}
