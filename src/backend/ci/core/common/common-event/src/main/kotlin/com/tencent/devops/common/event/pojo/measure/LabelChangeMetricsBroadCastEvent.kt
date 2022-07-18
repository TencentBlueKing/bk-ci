package com.tencent.devops.common.event.pojo.measure

import com.tencent.devops.common.event.enums.PipelineLabelChangeTypeEnum
import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import java.time.LocalDateTime

@Event(exchange = MQ.EXCHANGE_PIPELINE_LABEL_CHANGE_METRICS_DATA_SYNC_FANOUT)
data class LabelChangeMetricsBroadCastEvent(
    override val projectId: String,
    override val pipelineId: String = "",
    val userId: String? = null,
    val statisticsTime: LocalDateTime? = null,
    val type: PipelineLabelChangeTypeEnum,
    val pipelineLabelRelateInfos: List<PipelineLabelRelateInfo>
) : IMeasureEvent(projectId, pipelineId, pipelineId)
