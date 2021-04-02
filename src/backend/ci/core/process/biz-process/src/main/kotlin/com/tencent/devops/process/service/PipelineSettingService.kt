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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.model.process.tables.TPipelineSettingVersion
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.dao.PipelineSettingVersionDao
import com.tencent.devops.process.pojo.pipeline.PipelineSubscriptionType
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.pojo.setting.PipelineRunLockType.MULTIPLE
import com.tencent.devops.process.pojo.setting.PipelineSettingVersion
import com.tencent.devops.process.pojo.setting.Subscription
import com.tencent.devops.process.service.label.PipelineGroupVersionService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineSettingService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val pipelineSettingVersionDao: PipelineSettingVersionDao,
    private val pipelineGroupVersionService: PipelineGroupVersionService
) {

    fun userGetSettingVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int,
        channelCode: ChannelCode = ChannelCode.BS
    ): PipelineSettingVersion {
        val setting = pipelineSettingVersionDao.getSetting(dslContext, pipelineId, version)
        val groups = pipelineGroupVersionService.getGroups(userId, projectId, pipelineId)
        val labels = ArrayList<String>()
        groups.forEach {
            labels.addAll(it.labels)
        }
        return if (setting != null) {
            setting.map {
                with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
                    val successType = it.get(SUCCESS_TYPE).split(",").filter { i -> i.isNotBlank() }
                        .map { type -> PipelineSubscriptionType.valueOf(type) }.toSet()
                    val failType = it.get(FAIL_TYPE).split(",").filter { i -> i.isNotBlank() }
                        .map { type -> PipelineSubscriptionType.valueOf(type) }.toSet()
                    PipelineSettingVersion(
                        projectId,
                        pipelineId,
                        it.get(NAME),
                        it.get(DESC),
                        PipelineRunLockType.valueOf(it.get(RUN_LOCK_TYPE)),
                        Subscription(
                            successType,
                            it.get(SUCCESS_GROUP).split(",").toSet(),
                            it.get(SUCCESS_RECEIVER),
                            it.get(SUCCESS_WECHAT_GROUP_FLAG),
                            it.get(SUCCESS_WECHAT_GROUP),
                            it.get(SUCCESS_WECHAT_GROUP_MARKDOWN_FLAG),
                            it.get(SUCCESS_DETAIL_FLAG),
                            it.get(SUCCESS_CONTENT) ?: ""
                        ),
                        Subscription(
                            failType,
                            it.get(FAIL_GROUP).split(",").toSet(),
                            it.get(FAIL_RECEIVER),
                            it.get(FAIL_WECHAT_GROUP_FLAG),
                            it.get(FAIL_WECHAT_GROUP),
                            it.get(FAIL_WECHAT_GROUP_MARKDOWN_FLAG),
                            it.get(FAIL_DETAIL_FLAG),
                            it.get(FAIL_CONTENT) ?: ""
                        ),
                        labels,
                        DateTimeUtil.secondToMinute(it.get(WAIT_QUEUE_TIME_SECOND)),
                        it.get(MAX_QUEUE_SIZE),
                        it.get(VERSION)
                    )
                }
            }
        } else {
            val model = client.get(ServicePipelineResource::class).get(userId, projectId, pipelineId, channelCode).data
            val name = model?.name ?: "unknown pipeline name"
            val desc = model?.desc ?: ""
            PipelineSettingVersion(
                projectId,
                pipelineId,
                name,
                desc,
                MULTIPLE,
                Subscription(),
                Subscription(),
                labels
            )
        }
    }
}
