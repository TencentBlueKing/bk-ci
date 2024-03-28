package com.tencent.devops.process.yaml.transfer

import com.tencent.devops.process.yaml.v3.models.on.ManualRule
import com.tencent.devops.process.yaml.v3.models.on.PreTriggerOnV3
import kotlin.reflect.full.declaredMemberProperties

object VariableDefault {
    const val DEFAULT_TASK_TIME_OUT = 900L
    const val DEFAULT_WAIT_QUEUE_TIME_MINUTE = 480
    const val DEFAULT_RETRY_COUNT = 0
    const val DEFAULT_CONTINUE_WHEN_FAILED = false
    const val DEFAULT_PIPELINE_SETTING_MAX_QUEUE_SIZE = 10
    const val DEFAULT_JOB_PREPARE_TIMEOUT = 10
    const val DEFAULT_JOB_MAX_QUEUE_MINUTES = 60
    const val DEFAULT_JOB_MAX_RUNNING_MINUTES = 900
    const val DEFAULT_MUTEX_QUEUE_LENGTH = 5
    const val DEFAULT_MUTEX_TIMEOUT_MINUTES = 900
    const val DEFAULT_CHECKIN_TIMEOUT_HOURS = 24
    const val DEFAULT_MUTEX_QUEUE_ENABLE = false
    val DEFAULT_MANUAL_RULE = ManualRule(enable = null, canElementSkip = null, useLatestParameters = null)

    fun <T> T.nullIfDefault(value: T) = if (this == value) null else this

    val PreTriggerOnV3Properties = PreTriggerOnV3::class.declaredMemberProperties
}
