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

package com.tencent.devops.common.stream.constants

object StreamBinding {
    // 日志预处理事件
    const val BINDING_LOG_ORIGIN_EVENT_DESTINATION = "build.log.origin.event"

    // 日志预处理事件
    const val BINDING_LOG_STORAGE_EVENT_DESTINATION = "build.log.storage.event"

    // 日志构建状态事件
    const val BINDING_LOG_STATUS_EVENT_DESTINATION = "build.log.status.event"

    // 核心交换机及队列 ====================================
    const val QUEUE_PIPELINE_UPDATE = "engine.pipeline.update"

    const val QUEUE_PIPELINE_CREATE = "engine.pipeline.create"

    const val QUEUE_PIPELINE_DELETE = "engine.pipeline.delete"

    const val QUEUE_PIPELINE_RESTORE = "engine.pipeline.restore"

    const val QUEUE_PIPELINE_TIMER = "engine.pipeline.timer"

    const val QUEUE_PIPELINE_BUILD_START = "engine.pipeline.build.start"

    const val QUEUE_PIPELINE_BUILD_TASK_START = "engine.pipeline.build.task.start"

    const val QUEUE_PIPELINE_BUILD_STAGE = "engine.pipeline.build.stage"

    const val QUEUE_PIPELINE_BUILD_CONTAINER = "engine.pipeline.build.container"

    const val QUEUE_PIPELINE_BUILD_CANCEL = "engine.pipeline.build.cancel"

    const val QUEUE_PIPELINE_BUILD_FINISH = "engine.pipeline.build.finish"

    const val QUEUE_PIPELINE_PAUSE_TASK_EXECUTE = "engine.pipeline.pause.task.execute"

    // 监控 ====================================
    const val EXCHANGE_PIPELINE_MONITOR_DIRECT = "engine.pipeline.listener.monitor"

    // 接收上述广播的队列
    const val QUEUE_PIPELINE_BUILD_HEART_BEAT = "engine.pipeline.build.hb"
    // 构建产生的审核通知类队列
    const val QUEUE_PIPELINE_BUILD_NOTIFY = "engine.pipeline.build.notify"
    // 构建状态Websocket推送解耦
    const val QUEUE_PIPELINE_BUILD_WEBSOCKET = "engine.pipeline.build.websocket"

    // 构建排队广播exchange ====================================
    const val EXCHANGE_PIPELINE_BUILD_QUEUE_FANOUT = "engine.pipeline.build.queue.fanout"

    // 构建启动广播exchange ====================================
    const val EXCHANGE_PIPELINE_BUILD_START_FANOUT = "engine.pipeline.build.start.fanout"

    // 构建审核和检查步骤广播exchange ====================================
    const val EXCHANGE_PIPELINE_BUILD_REVIEW_FANOUT = "engine.pipeline.build.review.fanout"
    const val EXCHANGE_PIPELINE_BUILD_QUALITY_CHECK_FANOUT = "engine.pipeline.build.quality.check.fanout"

    // 构建结束后续广播exchange ====================================
    const val EXCHANGE_PIPELINE_BUILD_FINISH_FANOUT = "engine.pipeline.build.finish.fanout"

    // 构建取消后续广播exchange ====================================
    const val EXCHANGE_PIPELINE_BUILD_CANCEL_FANOUT = "engine.pipeline.build.cancel.fanout"

    // 插件结束后续广播exchange ====================================
    const val EXCHANGE_PIPELINE_BUILD_TASK_FINISH_FANOUT = "engine.pipeline.build.task.finish"

    // 定时变更广播exchange ====================================
    const val EXCHANGE_PIPELINE_TIMER_CHANGE_FANOUT = "engine.pipeline.timer.change"

    // 流水线扩展交换器 ====================================
    const val EXCHANGE_PIPELINE_EXTENDS_FANOUT = "fanout.engine.pipeline.extends"

    // 流水线模型分析队列
    const val ROUTE_PIPELINE_EXTENDS_MODEL = "r.engine.pipeline.extends.model"
    const val QUEUE_PIPELINE_EXTENDS_MODEL = "q.engine.pipeline.extends.model"

    // AGENT 构建机消息队列 ====================================
    const val EXCHANGE_AGENT_LISTENER_DIRECT = "e.engine.pipeline.agent.listener"
    const val ROUTE_AGENT_STARTUP = "r.engine.pipeline.agent.startup"
    const val QUEUE_AGENT_STARTUP = "q.engine.pipeline.agent.startup"
    const val ROUTE_AGENT_SHUTDOWN = "r.engine.pipeline.agent.shutdown"
    const val QUEUE_AGENT_SHUTDOWN = "q.engine.pipeline.agent.shutdown"

    // 无构建环境的Docker构建机启停消息队列 ====================================
    const val EXCHANGE_BUILD_LESS_AGENT_LISTENER_DIRECT = "e.engine.pipeline.bl.agent"
    const val ROUTE_BUILD_LESS_AGENT_STARTUP_DISPATCH = "r.engine.pipeline.bl.agent.dispatch.startup"
    const val QUEUE_BUILD_LESS_AGENT_STARTUP_DISPATCH = "q.engine.pipeline.bl.agent.dispatch.startup"
    const val ROUTE_BUILD_LESS_AGENT_SHUTDOWN_DISPATCH = "r.engine.pipeline.bl.agent.dispatch.shutdown"
    const val QUEUE_BUILD_LESS_AGENT_SHUTDOWN_DISPATCH = "q.engine.pipeline.bl.agent.dispatch.shutdown"
    const val QUEUE_BUILD_LESS_AGENT_STARTUP_PREFFIX = "q.engine.pipeline.bl.agent.startup."
    const val QUEUE_BUILD_LESS_AGENT_SHUTDOWN_PREFFIX = "q.engine.pipeline.bl.agent.shutdown."

    const val DEFAULT_BUILD_LESS_DOCKET_HOST_ROUTE_SUFFIX = "sys_default"
    // ================================================================================================

    // Git webhook事件回调
    const val QUEUE_GIT_COMMIT_CHECK = "engine.git.commit.check"
    const val QUEUE_GITHUB_PR = "engine.github.pr"

    // SVN代码仓库webhook请求回
    const val QUEUE_SVN_BUILD_REQUEST_EVENT = "engine.pipeline.hook.svn.event"

    // CodeGit代码仓库webhook请求回调
    const val QUEUE_GIT_BUILD_REQUEST_EVENT = "engine.pipeline.hook.git.event"

    // Gitlab代码仓库webhook请求回调
    const val QUEUE_GITLAB_BUILD_REQUEST_EVENT = "engine.pipeline.hook.gitlab.event"

    // Github代码仓库webhook请求回调
    const val QUEUE_GITHUB_BUILD_REQUEST_EVENT = "engine.pipeline.hook.github.event"

    // CodeTGit代码仓库webhook请求回调
    const val QUEUE_TGIT_BUILD_REQUEST_EVENT = "engine.pipeline.hook.tgit.event"

    // P4代码仓库webhook请求回调
    const val QUEUE_P4_BUILD_REQUEST_EVENT = "engine.pipeline.hook.p4.event"

    // 度量数据
    const val EXCHANGE_MEASURE_REQUEST_EVENT = "e.measure.request.event"
    const val ROUTE_MEASURE_REQUEST_EVENT = "r.measure.request.event"
    const val QUEUE_MEASURE_REQUEST_EVENT = "q.measure.request.event"

    // 流水线设置发生变化的事件广播
    const val EXCHANGE_PIPELINE_SETTING_CHANGE_FANOUT = "e.engine.pipeline.setting.change.fanout"
    const val ROUTE_PIPELINE_SETTING_CHANGE = "r.engine.pipeline.setting.change"
    const val QUEUE_PIPELINE_SETTING_CHANGE = "q.engine.pipeline.setting.change"

    // webSocket消息
    const val EXCHANGE_WEBSOCKET_TMP_FANOUT = "websocket.fanout"
    const val EXCHANGE_WEBSOCKET_TRANSFER_FANOUT = "websocket.transfer.fanout"
    const val BINDING_WEBSOCKET_SESSION_CLEAR_DESTINATION = "websocket.session.clear.fanout"

    // 回调
    const val EXCHANGE_PIPELINE_BUILD_CALL_BACK_FANOUT = "engine.pipeline.build.callback.fanout"

    // 蓝盾项目管理
    const val EXCHANGE_PROJECT_CREATE_FANOUT = "project.create.exchange.fanout"
    const val EXCHANGE_PROJECT_UPDATE_FANOUT = "project.update.exchange.fanout"
    const val EXCHANGE_PROJECT_UPDATE_LOGO_FANOUT = "project.update.logo.exchange.fanout"

    // 蓝盾监控数据上报事件广播
    const val EXCHANGE_ATOM_MONITOR_DATA_REPORT_FANOUT = "engine.atom.monitor.data.report.fanout"

    // 蓝盾构建结束后metrics数据上报事件广播
    const val EXCHANGE_BUILD_END_METRICS_DATA_REPORT_FANOUT = "metrics.engine.build.end.data.report.fanout"
    // 流水线标签变化metrics数据同步广播
    const val EXCHANGE_PIPELINE_LABEL_CHANGE_METRICS_DATA_SYNC_FANOUT =
        "metrics.pipeline.label.change.data.sync.fanout"

    // webhook锁
    const val QUEUE_GIT_WEBHOOK_UNLOCK_EVENT = "webhook.unlock.event"

    // 蓝盾管理员
    const val EXCHANGE_AUTH_REFRESH_FANOUT = "auth.refresh.exchange.fanout"

    // 流水线webhook commit记录
    const val EXCHANGE_PIPELINE_BUILD_COMMIT_FINISH_FANOUT = "engine.pipeline.build.commits.finish.fanout"

    // 流水线质量红线人工审核时间广播
    const val EXCHANGE_PIPELINE_BUILD_QUALITY_REVIEW_FANOUT = "quality.pipeline.build.review.fanout"

    // 度量数据上报队列：质量红线每日数据 + 编译加速每日数据 + 代码检查每日数据
    const val EXCHANGE_METRICS_STATISTIC_QUALITY_DAILY = "metrics.statistic.quality.daily"
    const val EXCHANGE_METRICS_STATISTIC_TURBO_DAILY = "metrics.statistic.turbo.daily"
    const val EXCHANGE_METRICS_STATISTIC_CODE_CHECK_DAILY = "metrics.statistic.code.check.daily"

    const val QUEUE_PIPELINE_STREAM_ENABLED = "engine.pipeline.stream.enabled"
}
