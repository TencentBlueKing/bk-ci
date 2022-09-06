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

package com.tencent.devops.metrics.listener

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.event.enums.PipelineLabelChangeTypeEnum
import com.tencent.devops.common.event.listener.Listener
import com.tencent.devops.common.event.pojo.measure.LabelChangeMetricsBroadCastEvent
import com.tencent.devops.metrics.service.SyncPipelineRelateLabelDataService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class LabelChangeMetricsDataSyncListener @Autowired constructor(
    private val syncPipelineRelateLabelDataService: SyncPipelineRelateLabelDataService
) : Listener<LabelChangeMetricsBroadCastEvent> {

    override fun execute(event: LabelChangeMetricsBroadCastEvent) {
        try {
            val pipelineLabelRelateInfos = event.pipelineLabelRelateInfos
            logger.info("Receive LabelChangeMetricsBroadCastEvent - $pipelineLabelRelateInfos|${event.type}")
            when (event.type) {
                PipelineLabelChangeTypeEnum.CREATE -> {
                    syncPipelineRelateLabelDataService.syncCreatePipelineRelateLabelData(
                        event.projectId,
                        event.pipelineId,
                        pipelineLabelRelateInfos
                    )
                }
                PipelineLabelChangeTypeEnum.DELETE -> {
                    syncPipelineRelateLabelDataService.syncDeletePipelineRelateLabelData(
                        event.projectId,
                        event.pipelineId,
                        pipelineLabelRelateInfos
                    )
                }
                PipelineLabelChangeTypeEnum.UPDATE -> {
                    syncPipelineRelateLabelDataService.syncUpdatePipelineRelateLabelData(
                        event.projectId,
                        event.pipelineId,
                        event.userId!!,
                        event.statisticsTime!!,
                        pipelineLabelRelateInfos
                    )
                }
            }
        } catch (ignored: Throwable) {
            logger.warn("Fail to sync the pipeline label data", ignored)
            throw ErrorCodeException(
                errorCode = CommonMessageCode.SYSTEM_ERROR,
                defaultMessage = "Fail to sync the pipeline label data"
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LabelChangeMetricsDataSyncListener::class.java)
    }
}
