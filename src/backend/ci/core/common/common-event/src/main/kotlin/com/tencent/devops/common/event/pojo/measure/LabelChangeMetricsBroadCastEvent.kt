package com.tencent.devops.common.event.pojo.measure

import com.tencent.devops.common.event.enums.PipelineLabelChangeTypeEnum
import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.stream.constants.StreamBinder
import com.tencent.devops.common.stream.constants.StreamBinding
import java.time.LocalDateTime

@Event(destination = StreamBinding.PIPELINE_LABEL_CHANGE_METRICS_DATA_SYNC_FANOUT, binder = StreamBinder.EXTEND_RABBIT)
data class LabelChangeMetricsBroadCastEvent(
    override val projectId: String,
    override val pipelineId: String = "",
    val userId: String? = null,
    val statisticsTime: LocalDateTime? = null,
    val type: PipelineLabelChangeTypeEnum,
    val pipelineLabelRelateInfos: List<PipelineLabelRelateInfo>
) : IMeasureEvent(projectId, pipelineId, pipelineId)
