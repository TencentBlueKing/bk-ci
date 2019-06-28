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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.event.dispatcher.pipeline.mq

object MQ {

    const val ENGINE_PROCESS_LISTENER_EXCHANGE = "e.engine.pipeline.listener"

    const val ROUTE_PIPELINE_UPDATE = "r.engine.pipeline.update"
    const val QUEUE_PIPELINE_UPDATE = "q.engine.pipeline.update"

    const val ROUTE_PIPELINE_CREATE = "r.engine.pipeline.create"
    const val QUEUE_PIPELINE_CREATE = "q.engine.pipeline.create"

    const val ROUTE_PIPELINE_DELETE = "r.engine.pipeline.delete"
    const val QUEUE_PIPELINE_DELETE = "q.engine.pipeline.delete"

    const val ROUTE_PIPELINE_TIMER = "r.engine.pipeline.timer"
    const val QUEUE_PIPELINE_TIMER = "q.engine.pipeline.timer"

    const val ROUTE_PIPELINE_BUILD_START = "r.engine.pipeline.build.start"
    const val QUEUE_PIPELINE_BUILD_START = "q.engine.pipeline.build.start"

    const val ROUTE_PIPELINE_BUILD_TASK_START = "r.engine.pipeline.build.task.start"
    const val QUEUE_PIPELINE_BUILD_TASK_START = "q.engine.pipeline.build.task.start"

    const val EXCHANGE_PIPELINE_MONITOR_DIRECT = "e.engine.pipeline.listener.monitor"
    const val ROUTE_PIPELINE_BUILD_MONITOR = "r.engine.pipeline.listener.monitor"
    const val QUEUE_PIPELINE_BUILD_MONITOR = "q.engine.pipeline.listener.monitor"

    const val ROUTE_PIPELINE_BUILD_HEART_BEAT = "r.engine.pipeline.build.hb"
    const val QUEUE_PIPELINE_BUILD_HEART_BEAT = "q.engine.pipeline.build.hb"

    const val ROUTE_PIPELINE_BUILD_CONTAINER_STARTUP = "r.engine.pipeline.build.container.startup"
    const val QUEUE_PIPELINE_BUILD_CONTAINER_STARTUP = "q.engine.pipeline.build.container.startup"

    const val ROUTE_PIPELINE_BUILD_STAGE = "r.engine.pipeline.build.stage"
    const val QUEUE_PIPELINE_BUILD_STAGE = "q.engine.pipeline.build.stage"

    const val ROUTE_PIPELINE_BUILD_CONTAINER = "r.engine.pipeline.build.container"
    const val QUEUE_PIPELINE_BUILD_CONTAINER = "q.engine.pipeline.build.container"

    const val ROUTE_PIPELINE_BUILD_CANCEL = "r.engine.pipeline.build.cancel"
    const val QUEUE_PIPELINE_BUILD_CANCEL = "q.engine.pipeline.build.cancel"
    const val ROUTE_PIPELINE_BUILD_FINISH = "r.engine.pipeline.build.finish"
    const val QUEUE_PIPELINE_BUILD_FINISH = "q.engine.pipeline.build.finish"

    // 构建结束后续广播exchange
    const val EXCHANGE_PIPELINE_BUILD_FINISH_FANOUT = "e.engine.pipeline.build.finish"
    // 接收上述广播的队列
    const val QUEUE_PIPELINE_BUILD_NOTIFY = "q.engine.pipeline.build.notify"
    const val QUEUE_PIPELINE_BUILD_FINISH_MEASURE = "q.engine.pipeline.build.measure"
    const val QUEUE_PIPELINE_BUILD_FINISH_CODE_WEBHOOK = "q.engine.pipeline.build.code.webhook"
    const val QUEUE_PIPELINE_BUILD_FINISH_ATOM_MARKET = "q.engine.pipeline.build.atom.market"

    // 定时变更广播exchange
    const val EXCHANGE_PIPELINE_TIMER_CHANGE_FANOUT = "e.engine.pipeline.timer.change"

    // 流水线扩展交换器
    const val EXCHANGE_PIPELINE_EXTENDS_FANOUT = "e.fanout.engine.pipeline.extends"
    // 流水线模型分析队列
    const val ROUTE_PIPELINE_EXTENDS_MODEL = "r.engine.pipeline.extends.model"
    const val QUEUE_PIPELINE_EXTENDS_MODEL = "q.engine.pipeline.extends.model"

    // AGENT 构建机消息队列
    const val EXCHANGE_AGENT_LISTENER_DIRECT = "e.engine.pipeline.agent.listener"
    const val ROUTE_AGENT_STARTUP = "r.engine.pipeline.agent.startup"
    const val QUEUE_AGENT_STARTUP = "q.engine.pipeline.agent.startup"
    const val ROUTE_AGENT_SHUTDOWN = "r.engine.pipeline.agent.shutdown"
    const val QUEUE_AGENT_SHUTDOWN = "q.engine.pipeline.agent.shutdown"

    // 无构建环境的Docker构建机启停消息队列
    const val EXCHANGE_BUILD_LESS_AGENT_LISTENER_DIRECT = "e.engine.pipeline.bl.agent"
    const val ROUTE_BUILD_LESS_AGENT_STARTUP_DISPATCH = "r.engine.pipeline.bl.agent.dispatch.startup"
    const val QUEUE_BUILD_LESS_AGENT_STARTUP_DISPATCH = "q.engine.pipeline.bl.agent.dispatch.startup"
    const val ROUTE_BUILD_LESS_AGENT_SHUTDOWN_DISPATCH = "r.engine.pipeline.bl.agent.dispatch.shutdown"
    const val QUEUE_BUILD_LESS_AGENT_SHUTDOWN_DISPATCH = "q.engine.pipeline.bl.agent.dispatch.shutdown"
    const val QUEUE_BUILD_LESS_AGENT_STARTUP_PREFFIX = "q.engine.pipeline.bl.agent.startup."
    const val QUEUE_BUILD_LESS_AGENT_SHUTDOWN_PREFFIX = "q.engine.pipeline.bl.agent.shutdown."

    const val DEFAULT_BUILD_LESS_DOCKET_HOST_ROUTE_SUFFIX = "sys_default"

    // 日志事件
    const val EXCHANGE_LOG_BUILD_EVENT = "e.engine.log.build.event"
    const val ROUTE_LOG_BUILD_EVENT = "r.engine.log.build.event"
    const val QUEUE_LOG_BUILD_EVENT = "q.engine.log.build.event"

    const val EXCHANGE_LOG_BATCH_BUILD_EVENT = "e.engine.log.batch.build.event"
    const val ROUTE_LOG_BATCH_BUILD_EVENT = "r.engine.log.batch.build.event"
    const val QUEUE_LOG_BATCH_BUILD_EVENT = "q.engine.log.batch.build.event"

    // 日志事件
    const val EXCHANGE_LOG_STATUS_BUILD_EVENT = "e.engine.log.status.build.event"
    const val ROUTE_LOG_STATUS_BUILD_EVENT = "r.engine.log.status.build.event"
    const val QUEUE_LOG_STATUS_BUILD_EVENT = "q.engine.log.status.build.event"

    // Git webhook事件回调
    const val EXCHANGE_GIT_COMMIT_CHECK = "e.engine.git.event"
    const val ROUTE_GIT_COMMIT_CHECK = "r.engine.git.commit.check"
    const val QUEUE_GIT_COMMIT_CHECK = "q.engine.git.commit.check"
    const val ROUTE_GITHUB_PR = "r.engine.github.pr"
    const val QUEUE_GITHUB_PR = "q.engine.github.pr"

    // 广播清理文件
    const val EXCHANGE_BKJOB_CLEAR_JOB_TMP_FANOUT = "e.bkjob.clear.file.fanout"
    const val ROUTE_BKJOB_CLEAR_JOB_TMP_EVENT = "r.bkjob.clear.file"
    const val QUEUE_BKJOB_CLEAR_JOB_TMP_EVENT = "q.bkjob.clear.file"

    // 广播流水线状态变化
    const val EXCHANGE_PIPELINE_STATUS_CHANGE_TMP_FANOUT = "e.pipeline.status.change.fanout"
    const val ROUTE_PIPELINE_STATUS_CHANGE_TMP_EVENT = "r.pipeline.status.change.file"
    const val QUEUE_PIPELINE_STATUS_CHANGE_TMP_EVENT = "q.pipeline.status.change.file"

    // 度量数据
    const val EXCHANGE_MEASURE_REQUEST_EVENT = "e.measure.request.event"
    const val ROUTE_MEASURE_REQUEST_EVENT = "r.measure.request.event"
    const val QUEUE_MEASURE_REQUEST_EVENT = "q.measure.request.event"

    // 流水线设置发生变化的事件广播
    const val EXCHANGE_PIPELINE_SETTING_CHANGE_FANOUT = "e.engine.pipeline.setting.change.fanout"
    const val ROUTE_PIPELINE_SETTING_CHANGE = "r.engine.pipeline.setting.change"
    const val QUEUE_PIPELINE_SETTING_CHANGE = "q.engine.pipeline.setting.change"
}
