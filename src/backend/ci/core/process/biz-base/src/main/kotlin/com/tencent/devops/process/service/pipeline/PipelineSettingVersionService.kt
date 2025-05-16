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
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.dao.PipelineSettingVersionDao
import com.tencent.devops.process.pojo.PipelineDetailInfo
import com.tencent.devops.process.pojo.setting.PipelineSettingVersion
import com.tencent.devops.process.service.PipelineAsCodeService
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.utils.PipelineVersionUtils
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("ComplexMethod")
class PipelineSettingVersionService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineSettingDao: PipelineSettingDao,
    private val pipelineSettingVersionDao: PipelineSettingVersionDao,
    private val pipelineAsCodeService: PipelineAsCodeService
) {

    /**
     * 获取指定版本的完整流水线设置（需要合并不属于版本管理的字段）
     */
    @Suppress("NestedBlockDepth")
    fun getPipelineSetting(
        projectId: String,
        pipelineId: String,
        userId: String?,
        detailInfo: PipelineDetailInfo?,
        channelCode: ChannelCode = ChannelCode.BS,
        version: Int
    ): PipelineSetting {
        // 获取正式版本的流水线设置
        var settingInfo = pipelineSettingDao.getSetting(dslContext, projectId, pipelineId)

        // 获取已生效的流水线的标签和分组
        val labels = ArrayList<String>()
        val labelNames = ArrayList<String>()
        userId?.let {
            pipelineGroupService.getGroups(userId, projectId, pipelineId).forEach {
                labels.addAll(it.labels)
                labelNames.addAll(it.labelNames)
            }
        }

        if (settingInfo == null) {
            // 如果没有正式版本的设置，则从流水线信息获取关键信息，生成新的流水线配置
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
                labelNames = labelNames,
                pipelineAsCodeSettings = PipelineAsCodeSettings()
            )
        } else {
            // 如果有正式版本，则将匹配好的标签替换进配置
            settingInfo.labels = labels
            settingInfo.labelNames = labelNames
        }

        if (version > 0) { // #671 目前只接受通知设置的版本管理, 其他属于公共设置不接受版本管理
            // #8161 除了通知以外增加了其他用户配置作为版本管理
            getPipelineSettingVersion(projectId, pipelineId, version)?.let { ve ->
                settingInfo.version = ve.version
                settingInfo.successSubscriptionList = ve.successSubscriptionList ?: settingInfo.successSubscriptionList
                settingInfo.failSubscriptionList = ve.failSubscriptionList ?: settingInfo.failSubscriptionList
                // 这里不应该出现错误的流水线名，但保留历史留下的处理方式
                settingInfo.pipelineName = ve.pipelineName ?: settingInfo.pipelineName
                settingInfo.labels = ve.labels ?: labels
                settingInfo.desc = ve.desc ?: settingInfo.desc
                settingInfo.buildNumRule = ve.buildNumRule ?: settingInfo.buildNumRule
                // #8161 如果是PAC发布前产生的数据，则流水线名称为空，可以用正式配置覆盖
                settingInfo.runLockType = if (ve.pipelineName.isNullOrBlank()) {
                    settingInfo.runLockType
                } else ve.runLockType ?: settingInfo.runLockType
                settingInfo.concurrencyGroup = ve.concurrencyGroup ?: settingInfo.concurrencyGroup
                settingInfo.concurrencyCancelInProgress = ve.concurrencyCancelInProgress
                    ?: settingInfo.concurrencyCancelInProgress
                settingInfo.waitQueueTimeMinute = ve.waitQueueTimeMinute ?: settingInfo.waitQueueTimeMinute
                settingInfo.maxQueueSize = ve.maxQueueSize ?: settingInfo.maxQueueSize
                settingInfo.maxConRunningQueueSize = if (ve.maxConRunningQueueSize != -1) {
                    ve.maxConRunningQueueSize ?: settingInfo.maxConRunningQueueSize
                } else {
                    null
                }
                settingInfo.pipelineAsCodeSettings = ve.pipelineAsCodeSettings
                settingInfo.failIfVariableInvalid = ve.failIfVariableInvalid
            }
            // 来自前端的请求中，版本中的可能还不是正式生效的，如果和正式配置中有差异则重新获取名称
            if (settingInfo.labels.isNotEmpty() && settingInfo.labels != labels && userId != null) {
                labelNames.clear()
                pipelineGroupService.getGroups(userId, projectId).forEach { group ->
                    group.labels.forEach { label ->
                        if (settingInfo.labels.contains(label.id)) labelNames.add(label.name)
                    }
                }
                settingInfo.labelNames = labelNames
            }
            settingInfo.pipelineAsCodeSettings = pipelineAsCodeService.getPipelineAsCodeSettings(
                projectId = projectId,
                settingInfo.pipelineAsCodeSettings
            )
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
        context: DSLContext? = null,
        projectId: String,
        pipelineId: String
    ): PipelineSettingVersion? {
        return pipelineSettingVersionDao.getLatestSettingVersion(
            dslContext = context ?: dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: pipelineSettingDao.getSetting(
            context ?: dslContext, projectId, pipelineId
        )?.let { PipelineSettingVersion.convertFromSetting(it) }
    }

    fun getSettingVersionAfterUpdate(
        projectId: String,
        pipelineId: String,
        updateVersion: Boolean,
        setting: PipelineSetting
    ): Int {
        return getLatestSettingVersion(
            projectId = projectId,
            pipelineId = pipelineId
        )?.let { latest ->
            if (updateVersion) PipelineVersionUtils.getSettingVersion(
                currVersion = latest.version,
                originSetting = latest,
                newSetting = PipelineSettingVersion.convertFromSetting(setting)
            ) else latest.version
        } ?: 1
    }
}
