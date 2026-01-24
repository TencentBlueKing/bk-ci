package com.tencent.devops.process.constant

/**
 * 度量标签常量
 */
object MeasureConstant {
    // 定时任务执行耗时
    const val NAME_PIPELINE_CRON_SCHEDULE_DELAY = "pipeline.cron.schedule.delay"

    // 定时任务触发耗时
    const val NAME_PIPELINE_CRON_EXECUTE_DELAY = "pipeline.cron.execute.delay"

    // webhook触发耗时
    const val PIPELINE_SCM_WEBHOOK_EXECUTE_TIME = "pipeline.scm.webhook.execute.time"
    // 状态
    const val TAG_SCM_WEBHOOK_TRIGGER_STATUS = "status"
    // 是否是yaml
    const val TAG_SCM_WEBHOOK_TRIGGER_YAML = "yaml"

    // 是否是老的webhook
    const val TAG_SCM_WEBHOOK_TRIGGER_OLD = "old"
}
