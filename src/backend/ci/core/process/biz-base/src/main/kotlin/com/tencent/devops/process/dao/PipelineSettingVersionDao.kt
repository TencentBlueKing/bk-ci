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

package com.tencent.devops.process.dao

import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.model.process.tables.TPipelineSettingVersion
import com.tencent.devops.model.process.tables.records.TPipelineSettingVersionRecord
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.util.NotifyTemplateUtils
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Suppress("LongParameterList")
@Repository
class PipelineSettingVersionDao {

    // 新流水线创建的时候，设置默认的通知配置。
    fun insertNewSetting(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int = 1,
        isTemplate: Boolean = false,
        successNotifyTypes: String = "",
        failNotifyTypes: String = "${NotifyType.EMAIL.name},${NotifyType.RTX.name}",
        id: Long? = null
    ): Int {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                SUCCESS_RECEIVER,
                FAIL_RECEIVER,
                SUCCESS_GROUP,
                FAIL_GROUP,
                SUCCESS_TYPE,
                FAIL_TYPE,
                SUCCESS_CONTENT,
                FAIL_CONTENT,
                IS_TEMPLATE,
                VERSION,
                ID
            )
                .values(
                    projectId,
                    pipelineId,
                    "\${$PIPELINE_START_USER_NAME}",
                    "\${$PIPELINE_START_USER_NAME}",
                    "",
                    "",
                    successNotifyTypes,
                    failNotifyTypes,
                    NotifyTemplateUtils.getCommonShutdownSuccessContent(),
                    NotifyTemplateUtils.getCommonShutdownFailureContent(),
                    isTemplate,
                    version,
                    id
                )
                .execute()
        }
    }

    fun saveSetting(
        dslContext: DSLContext,
        setting: PipelineSetting,
        version: Int,
        isTemplate: Boolean = false,
        id: Long? = null
    ): Int {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                SUCCESS_RECEIVER,
                FAIL_RECEIVER,
                SUCCESS_GROUP,
                FAIL_GROUP,
                SUCCESS_TYPE,
                FAIL_TYPE,
                FAIL_WECHAT_GROUP_FLAG,
                FAIL_WECHAT_GROUP,
                SUCCESS_WECHAT_GROUP_FLAG,
                SUCCESS_WECHAT_GROUP,
                SUCCESS_DETAIL_FLAG,
                FAIL_DETAIL_FLAG,
                SUCCESS_CONTENT,
                FAIL_CONTENT,
                IS_TEMPLATE,
                VERSION,
                ID
            )
                .values(
                    setting.projectId,
                    setting.pipelineId,
                    setting.successSubscription.users,
                    setting.failSubscription.users,
                    setting.successSubscription.groups.joinToString(","),
                    setting.failSubscription.groups.joinToString(","),
                    setting.successSubscription.types.joinToString(",") { it.name },
                    setting.failSubscription.types.joinToString(",") { it.name },
                    setting.failSubscription.wechatGroupFlag,
                    setting.failSubscription.wechatGroup,
                    setting.successSubscription.wechatGroupFlag,
                    setting.successSubscription.wechatGroup,
                    setting.successSubscription.detailFlag,
                    setting.failSubscription.detailFlag,
                    setting.successSubscription.content,
                    setting.failSubscription.content,
                    isTemplate,
                    version,
                    id
                )
                .execute()
        }
    }

    fun getSetting(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int
    ): TPipelineSettingVersionRecord? {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VERSION.eq(version))
                .and(PROJECT_ID.eq(projectId))
                .fetchOne()
        }
    }

    fun getSettingByPipelineIds(
        dslContext: DSLContext,
        pipelineIds: List<String>
    ): Result<TPipelineSettingVersionRecord> {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.`in`(pipelineIds))
                .fetch()
        }
    }

    fun batchUpdate(dslContext: DSLContext, tPipelineSettingVersionRecords: List<TPipelineSettingVersionRecord>) {
        dslContext.batchUpdate(tPipelineSettingVersionRecords).execute()
    }

    fun deleteAllVersion(dslContext: DSLContext, projectId: String, pipelineId: String): Int {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            return dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deleteByVer(dslContext: DSLContext, projectId: String, pipelineId: String, version: Int): Int {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            return dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VERSION.eq(version))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deleteEarlyVersion(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        currentVersion: Int,
        maxPipelineResNum: Int
    ): Int {
        return with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VERSION.le(currentVersion - maxPipelineResNum))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }
}
