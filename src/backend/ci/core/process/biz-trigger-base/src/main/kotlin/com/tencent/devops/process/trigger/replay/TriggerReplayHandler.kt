package com.tencent.devops.process.trigger.replay

import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.trigger.PipelineTriggerType
import com.tencent.devops.process.webhook.pojo.event.commit.ReplayWebhookEvent

/**
 * 触发事件重放处理器，按触发类型实现
 */
interface TriggerReplayHandler {

    fun support(triggerType: PipelineTriggerType): Boolean

    /**
     * 重放：由处理器自行投递 MQ 异步触发
     *
     * @param replayEvent pipelineId 为空表示重放事件下的全部流水线
     * @param replayTriggerEvent 重放生成的触发事件
     */
    fun replay(replayEvent: ReplayWebhookEvent, replayTriggerEvent: PipelineTriggerEvent)

    /**
     * 重新构建：在当前线程内按最新版本完成匹配和启动，触发结果同时记录在触发详情中
     *
     * @param replayEvent pipelineId 不能为空
     * @param replayTriggerEvent 重放生成的触发事件
     * @return 触发成功的构建ID，未触发返回 null
     */
    fun replayBuild(replayEvent: ReplayWebhookEvent, replayTriggerEvent: PipelineTriggerEvent): String?
}
