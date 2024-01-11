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

package com.tencent.devops.process.service.pipeline

import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
import com.tencent.devops.common.pipeline.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_DEFAULT
import com.tencent.devops.common.pipeline.utils.PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_DEFAULT
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.dao.PipelineSettingVersionDao
import com.tencent.devops.process.pojo.PipelineDetailInfo
import com.tencent.devops.process.pojo.setting.PipelineSettingVersion
import com.tencent.devops.process.service.label.PipelineGroupService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineSettingVersionService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineSettingDao: PipelineSettingDao,
    private val pipelineSettingVersionDao: PipelineSettingVersionDao
) {

    fun getPipelineSetting(
        projectId: String,
        pipelineId: String,
        userId: String,
        detailInfo: PipelineDetailInfo?,
        channelCode: ChannelCode = ChannelCode.BS,
        version: Int
    ): PipelineSetting {
        // 正式版本的流水线设置
        var settingInfo = pipelineSettingDao.getSetting(dslContext, projectId, pipelineId)
        val groups = pipelineGroupService.getGroups(userId, projectId, pipelineId)
        val labels = ArrayList<String>()
        groups.forEach {
            labels.addAll(it.labels)
        }
        if (settingInfo == null) {
            val (pipelineName, pipelineDesc) = detailInfo?.let {
                Pair(it.pipelineName, it.pipelineDesc)
            } ?: client.get(ServicePipelineResource::class).getPipelineInfo(
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = channelCode
            ).data?.let {
                Pair(it.pipelineName, it.pipelineDesc)
            } ?: Pair(null, null)
            settingInfo = PipelineSetting(
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = pipelineName ?: "unknown pipeline name",
                desc = pipelineDesc ?: "",
                runLockType = PipelineRunLockType.MULTIPLE,
                successSubscription = Subscription(),
                failSubscription = Subscription(),
                labels = labels,
                pipelineAsCodeSettings = PipelineAsCodeSettings()
            )
        } else {
            settingInfo.labels = labels
        }

        if (version > 0) { // #671 目前只接受通知设置的版本管理, 其他属于公共设置不接受版本管理
            // #8161 除了通知以外增加了其他用户配置作为版本管理
            getPipelineSettingVersion(projectId, pipelineId, version)?.let { ve ->
                settingInfo.successSubscriptionList = ve.successSubscriptionList
                settingInfo.failSubscriptionList = ve.failSubscriptionList
                settingInfo.labels = ve.labels ?: listOf()
                settingInfo.desc = ve.desc
                settingInfo.buildNumRule = ve.buildNumRule
                settingInfo.runLockType = ve.runLockType
                settingInfo.waitQueueTimeMinute = ve.waitQueueTimeMinute
                    ?: PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_DEFAULT
                settingInfo.maxQueueSize = ve.maxQueueSize ?: PIPELINE_SETTING_MAX_QUEUE_SIZE_DEFAULT
                settingInfo.concurrencyGroup = ve.concurrencyGroup
                settingInfo.concurrencyCancelInProgress = ve.concurrencyCancelInProgress ?: false
                settingInfo.pipelineAsCodeSettings = ve.pipelineAsCodeSettings
            }
        }

        return settingInfo
    }

    fun getPipelineSettingVersion(
        projectId: String,
        pipelineId: String,
        version: Int
    ): PipelineSettingVersion? {
        return pipelineSettingVersionDao.getSettingVersion(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        )
    }

    fun getLatestSettingVersion(
        projectId: String,
        pipelineId: String
    ): PipelineSettingVersion? {
        return pipelineSettingVersionDao.getLatestSettingVersion(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }
}
