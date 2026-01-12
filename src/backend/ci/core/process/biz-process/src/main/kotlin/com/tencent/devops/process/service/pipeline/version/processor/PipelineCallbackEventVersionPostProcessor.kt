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

package com.tencent.devops.process.service.pipeline.version.processor

import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.event.PipelineCallbackEvent
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.dao.PipelineCallbackDao
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 流水线创建后,回调事件创建
 */
@Service
class PipelineCallbackEventVersionPostProcessor @Autowired constructor(
    private val pipelineCallbackDao: PipelineCallbackDao
) : PipelineVersionCreatePostProcessor {

    @Value("\${project.callback.secretParam.aes-key:project_callback_aes_key}")
    private val aesKey = ""

    override fun postProcessInTransactionVersionCreate(
        transactionContext: DSLContext,
        context: PipelineVersionCreateContext,
        pipelineResourceVersion: PipelineResourceVersion,
        pipelineSetting: PipelineSetting
    ) {
        with(context) {
            // 更新流水线,只有正式版本才需要更新回调
            if (pipelineInfo != null && pipelineResourceVersion.status != VersionStatus.RELEASED) {
                return
            }
            // 初始化流水线单体回调
            savePipelineCallback(
                dslContext = transactionContext,
                userId = userId,
                projectId = pipelineBasicInfo.projectId,
                pipelineId = pipelineBasicInfo.pipelineId,
                events = pipelineModelBasicInfo.events
            )
        }
    }

    /**
     * 保存流水线单体回调记录
     */
    private fun savePipelineCallback(
        events: Map<String, PipelineCallbackEvent>?,
        pipelineId: String,
        projectId: String,
        dslContext: DSLContext,
        userId: String
    ) {
        if (events.isNullOrEmpty()) return
        val existEventNames = pipelineCallbackDao.list(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        ).map { it.name }.toSet()
        if (existEventNames.isNotEmpty()) {
            val needDelNames = existEventNames.subtract(events.keys).toSet()
            pipelineCallbackDao.delete(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                names = needDelNames
            )
        }
        // 保存回调事件
        pipelineCallbackDao.save(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            list = events.map { (key, value) ->
                value.copy(secretToken = value.secretToken?.let { AESUtil.encrypt(aesKey, it) })
            }
        )
    }
}
