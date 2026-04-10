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

package com.tencent.devops.process.service.pipeline

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.db.pojo.ARCHIVE_SHARDING_DSL_CONTEXT
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.dao.PipelineSettingDraftVersionDao
import com.tencent.devops.process.dao.PipelineSettingVersionDao
import com.tencent.devops.process.pojo.PipelineDetailInfo
import com.tencent.devops.process.pojo.setting.PipelineSettingDraftVersion
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
    private val pipelineAsCodeService: PipelineAsCodeService,
    private val pipelineSettingDraftVersionDao: PipelineSettingDraftVersionDao
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
        version: Int,
        archiveFlag: Boolean? = false
    ): PipelineSetting {
        val finalDslContext = CommonUtils.getJooqDslContext(archiveFlag, ARCHIVE_SHARDING_DSL_CONTEXT)
        // 获取正式版本的流水线设置
        var settingInfo = pipelineSettingDao.getSetting(finalDslContext, projectId, pipelineId)

        // 获取已生效的流水线的标签和分组
        val labels = ArrayList<String>()
        val labelNames = ArrayList<String>()
        userId?.let {
            pipelineGroupService.getGroups(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                archiveFlag = archiveFlag
            ).forEach {
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
                channelCode = channelCode,
                archiveFlag = archiveFlag
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
            getPipelineSettingVersion(
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                queryDslContext = finalDslContext
            )?.let { ve ->
                mergeVersionSetting(
                    projectId = projectId,
                    userId = userId,
                    settingInfo = settingInfo,
                    settingVersion = ve,
                    labels = labels,
                    labelNames = labelNames
                )
            }
        }

        return settingInfo
    }

    fun getPipelineSettingVersion(
        projectId: String,
        pipelineId: String,
        version: Int,
        queryDslContext: DSLContext? = null
    ): PipelineSettingVersion? {
        return pipelineSettingVersionDao.getSettingVersion(
            dslContext = queryDslContext ?: dslContext,
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

    fun getPipelineSettingByDraftVersion(
        projectId: String,
        pipelineId: String,
        version: Int,
        draftVersion: Int
    ): PipelineSetting {
        val labels = ArrayList<String>()
        val labelNames = ArrayList<String>()
        val settingInfo = pipelineSettingDao.getSetting(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.PIPELINE_SETTING_NOT_EXISTS
        )
        pipelineSettingDraftVersionDao.get(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            draftVersion = draftVersion
        )?.let {
            val draftSettingVersion = PipelineSettingDraftVersion.convertFromDraftVersion(it)
            mergeVersionSetting(
                projectId = projectId,
                userId = null,
                settingInfo = settingInfo,
                settingVersion = draftSettingVersion,
                labels = labels,
                labelNames = labelNames
            )
        }
        return settingInfo
    }

    @Suppress("NestedBlockDepth")
    private fun mergeVersionSetting(
        projectId: String,
        userId: String?,
        settingInfo: PipelineSetting,
        settingVersion: PipelineSettingVersion,
        labels: List<String>,
        labelNames: MutableList<String>
    ) {
        settingInfo.version = settingVersion.version
        settingInfo.successSubscriptionList =
            settingVersion.successSubscriptionList ?: settingInfo.successSubscriptionList
        settingInfo.failSubscriptionList = settingVersion.failSubscriptionList ?: settingInfo.failSubscriptionList
        settingInfo.pipelineName = settingVersion.pipelineName ?: settingInfo.pipelineName
        settingInfo.labels = settingVersion.labels ?: labels
        settingInfo.desc = settingVersion.desc ?: settingInfo.desc
        settingInfo.buildNumRule = settingVersion.buildNumRule ?: settingInfo.buildNumRule
        settingInfo.runLockType = if (settingVersion.pipelineName.isNullOrBlank()) {
            settingInfo.runLockType
        } else {
            settingVersion.runLockType ?: settingInfo.runLockType
        }
        settingInfo.concurrencyGroup = settingVersion.concurrencyGroup ?: settingInfo.concurrencyGroup
        settingInfo.concurrencyCancelInProgress = settingVersion.concurrencyCancelInProgress
            ?: settingInfo.concurrencyCancelInProgress
        settingInfo.waitQueueTimeMinute = settingVersion.waitQueueTimeMinute ?: settingInfo.waitQueueTimeMinute
        settingInfo.maxQueueSize = settingVersion.maxQueueSize ?: settingInfo.maxQueueSize
        settingInfo.maxConRunningQueueSize = if (settingVersion.maxConRunningQueueSize != -1) {
            settingVersion.maxConRunningQueueSize ?: settingInfo.maxConRunningQueueSize
        } else {
            null
        }
        settingInfo.pipelineAsCodeSettings = settingVersion.pipelineAsCodeSettings
        settingInfo.failIfVariableInvalid = settingVersion.failIfVariableInvalid
        settingInfo.buildCancelPolicy = settingVersion.buildCancelPolicy ?: settingInfo.buildCancelPolicy
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
}
