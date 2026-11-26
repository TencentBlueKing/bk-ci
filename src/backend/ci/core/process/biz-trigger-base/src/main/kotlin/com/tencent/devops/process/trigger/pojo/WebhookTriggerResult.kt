package com.tencent.devops.process.trigger.pojo

import com.tencent.devops.process.pojo.trigger.PipelineTriggerReasonDetail

/**
 * 单条流水线触发结果
 *
 * @param buildId 触发成功的构建ID，未触发为 null
 * @param reasonDetail 未触发的原因，与触发明细中记录的一致，未记录明细时为 null
 */
data class WebhookTriggerResult(
    val buildId: String? = null,
    val reasonDetail: PipelineTriggerReasonDetail? = null
)
