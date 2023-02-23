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

package com.tencent.devops.common.event.dispatcher.pipeline.mq

object MQ {
    // 核心交换机及队列 ====================================
    const val ENGINE_PROCESS_LISTENER_EXCHANGE = "e.engine.pipeline.listener"

    const val ROUTE_PIPELINE_UPDATE = "r.engine.pipeline.update"
    const val QUEUE_PIPELINE_UPDATE = "q.engine.pipeline.update"

    const val ROUTE_PIPELINE_CREATE = "r.engine.pipeline.create"
    const val QUEUE_PIPELINE_CREATE = "q.engine.pipeline.create"

    const val ROUTE_PIPELINE_DELETE = "r.engine.pipeline.delete"
    const val QUEUE_PIPELINE_DELETE = "q.engine.pipeline.delete"

    const val ROUTE_PIPELINE_RESTORE = "r.engine.pipeline.restore"
    const val QUEUE_PIPELINE_RESTORE = "q.engine.pipeline.restore"

    const val ROUTE_PIPELINE_TIMER = "r.engine.pipeline.timer"
    const val QUEUE_PIPELINE_TIMER = "q.engine.pipeline.timer"

    const val ROUTE_PIPELINE_BUILD_START = "r.engine.pipeline.build.start"
    const val QUEUE_PIPELINE_BUILD_START = "q.engine.pipeline.build.start"

    const val ROUTE_PIPELINE_BUILD_TASK_START = "r.engine.pipeline.build.task.start"
    const val QUEUE_PIPELINE_BUILD_TASK_START = "q.engine.pipeline.build.task.start"

    const val ROUTE_PIPELINE_BUILD_STAGE = "r.engine.pipeline.build.stage"
    const val QUEUE_PIPELINE_BUILD_STAGE = "q.engine.pipeline.build.stage"

    const val ROUTE_PIPELINE_BUILD_CONTAINER = "r.engine.pipeline.build.container"
    const val QUEUE_PIPELINE_BUILD_CONTAINER = "q.engine.pipeline.build.container"

    const val ROUTE_PIPELINE_BUILD_CANCEL = "r.engine.pipeline.build.cancel"
    const val QUEUE_PIPELINE_BUILD_CANCEL = "q.engine.pipeline.build.cancel"
    const val ROUTE_PIPELINE_BUILD_FINISH = "r.engine.pipeline.build.finish"
    const val QUEUE_PIPELINE_BUILD_FINISH = "q.engine.pipeline.build.finish"

    const val ROUTE_PIPELINE_PAUSE_TASK_EXECUTE = "r.engine.pipeline.pause.task.execute"
    const val QUEUE_PIPELINE_PAUSE_TASK_EXECUTE = "q.engine.pipeline.pause.task.execute"

    // 监控 ====================================
    const val EXCHANGE_PIPELINE_MONITOR_DIRECT = "e.engine.pipeline.listener.monitor"

    // 接收上述广播的队列
    const val ROUTE_PIPELINE_BUILD_MONITOR = "r.engine.pipeline.listener.monitor"
    const val QUEUE_PIPELINE_BUILD_MONITOR = "q.engine.pipeline.listener.monitor"
    const val ROUTE_PIPELINE_BUILD_HEART_BEAT = "r.engine.pipeline.build.hb"
    const val QUEUE_PIPELINE_BUILD_HEART_BEAT = "q.engine.pipeline.build.hb"
    // 构建产生的审核通知类队列
    const val ROUTE_PIPELINE_BUILD_NOTIFY = "r.engine.pipeline.build.notify"
    const val QUEUE_PIPELINE_BUILD_NOTIFY = "q.engine.pipeline.build.notify"
    // 构建状态Websocket推送解耦
    const val ROUTE_PIPELINE_BUILD_WEBSOCKET = "r.engine.pipeline.build.websocket"
    const val QUEUE_PIPELINE_BUILD_WEBSOCKET = "q.engine.pipeline.build.websocket"

    // 构建排队广播exchange ====================================
    const val EXCHANGE_PIPELINE_BUILD_QUEUE_FANOUT = "e.engine.pipeline.build.queue.fanout"

    // 接受排队广播的队列
    const val QUEUE_PIPELINE_BUILD_QUEUE_CODE_WEBHOOK = "q.engine.pipeline.build.queue.code.webhook"

    // 构建启动广播exchange ====================================
    const val EXCHANGE_PIPELINE_BUILD_START_FANOUT = "e.engine.pipeline.build.start.fanout"
    const val QUEUE_PIPELINE_BUILD_START_DISPATCHER = "q.engine.pipeline.build.start.dispatcher"
    const val QUEUE_PIPELINE_BUILD_START_WEBHOOK_QUEUE = "q.engine.pipeline.build.start.webhook.queue"

    // 构建审核和检查步骤广播exchange ====================================
    const val EXCHANGE_PIPELINE_BUILD_REVIEW_FANOUT = "e.engine.pipeline.build.review.fanout"
    const val EXCHANGE_PIPELINE_BUILD_QUALITY_CHECK_FANOUT = "e.engine.pipeline.build.quality.check.fanout"

    // 构建结束后续广播exchange ====================================
    const val EXCHANGE_PIPELINE_BUILD_FINISH_FANOUT = "e.engine.pipeline.build.finish"

    // 构建取消后续广播exchange ====================================
    const val EXCHANGE_PIPELINE_BUILD_CANCEL_FANOUT = "e.engine.pipeline.build.cancel"

    // 接收上述广播的队列
    const val QUEUE_PIPELINE_BUILD_FINISH_MEASURE = "q.engine.pipeline.build.measure"
    const val QUEUE_PIPELINE_BUILD_FINISH_CODE_WEBHOOK = "q.engine.pipeline.build.code.webhook"
    const val QUEUE_PIPELINE_BUILD_FINISH_ATOM_MARKET = "q.engine.pipeline.build.atom.market"
    const val QUEUE_PIPELINE_BUILD_FINISH_LAMBDA = "q.engine.pipeline.build.lambda"
    const val QUEUE_PIPELINE_BUILD_FINISH_GITCI = "q.engine.pipeline.build.gitci"
    const val QUEUE_PIPELINE_BUILD_FINISH_LOG = "q.engine.pipeline.build.log"
    const val QUEUE_PIPELINE_BUILD_FINISH_SUBPIPEINE = "q.engine.pipeline.build.subpipeline"
    const val QUEUE_PIPELINE_BUILD_FINISH_WEBHOOK_QUEUE = "q.engine.pipeline.build.finish.webhook.queue"
    const val QUEUE_PIPELINE_BUILD_FINISH_NOTIFY_QUEUE = "q.engine.pipeline.build.finish.notify.queue"

    const val QUEUE_PIPELINE_BUILD_FINISH_EXT = "q.engine.pipeline.build.finish.ext"
    const val QUEUE_PIPELINE_BUILD_FINISH_DISPATCHER = "q.engine.pipeline.build.dispatcher"

    // 插件结束后续广播exchange ====================================
    const val EXCHANGE_PIPELINE_BUILD_ELEMENT_FINISH_FANOUT = "e.engine.pipeline.build.element.finish"

    // 接收上述广播的队列
    const val QUEUE_PIPELINE_BUILD_ELEMENT_FINISH_LAMBDA = "q.engine.pipeline.build.element.lambda"

    // 定时变更广播exchange ====================================
    const val EXCHANGE_PIPELINE_TIMER_CHANGE_FANOUT = "e.engine.pipeline.timer.change"

    // 流水线扩展交换器 ====================================
    const val EXCHANGE_PIPELINE_EXTENDS_FANOUT = "e.fanout.engine.pipeline.extends"

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

    // SVN代码仓库webhook请求回调
    const val EXCHANGE_SVN_BUILD_REQUEST_EVENT = "e.engine.pipeline.hook.svn.event"
    const val ROUTE_SVN_BUILD_REQUEST_EVENT = "r.engine.pipeline.hook.svn.event"
    const val QUEUE_SVN_BUILD_REQUEST_EVENT = "q.engine.pipeline.hook.svn.event"

    // CodeGit代码仓库webhook请求回调
    const val EXCHANGE_GIT_BUILD_REQUEST_EVENT = "e.engine.pipeline.hook.git.event"
    const val ROUTE_GIT_BUILD_REQUEST_EVENT = "r.engine.pipeline.hook.git.event"
    const val QUEUE_GIT_BUILD_REQUEST_EVENT = "q.engine.pipeline.hook.git.event"

    // Gitlab代码仓库webhook请求回调
    const val EXCHANGE_GITLAB_BUILD_REQUEST_EVENT = "e.engine.pipeline.hook.gitlab.event"
    const val ROUTE_GITLAB_BUILD_REQUEST_EVENT = "r.engine.pipeline.hook.gitlab.event"
    const val QUEUE_GITLAB_BUILD_REQUEST_EVENT = "q.engine.pipeline.hook.gitlab.event"

    // Github代码仓库webhook请求回调
    const val EXCHANGE_GITHUB_BUILD_REQUEST_EVENT = "e.engine.pipeline.hook.github.event"
    const val ROUTE_GITHUB_BUILD_REQUEST_EVENT = "r.engine.pipeline.hook.github.event"
    const val QUEUE_GITHUB_BUILD_REQUEST_EVENT = "q.engine.pipeline.hook.github.event"

    // CodeTGit代码仓库webhook请求回调
    const val EXCHANGE_TGIT_BUILD_REQUEST_EVENT = "e.engine.pipeline.hook.tgit.event"
    const val ROUTE_TGIT_BUILD_REQUEST_EVENT = "r.engine.pipeline.hook.tgit.event"
    const val QUEUE_TGIT_BUILD_REQUEST_EVENT = "q.engine.pipeline.hook.tgit.event"

    // P4代码仓库webhook请求回调
    const val EXCHANGE_P4_BUILD_REQUEST_EVENT = "e.engine.pipeline.hook.p4.event"
    const val ROUTE_P4_BUILD_REQUEST_EVENT = "r.engine.pipeline.hook.p4.event"
    const val QUEUE_P4_BUILD_REQUEST_EVENT = "q.engine.pipeline.hook.p4.event"

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

    // webSocket消息
    const val EXCHANGE_WEBSOCKET_TMP_FANOUT = "e.websocket.fanout"
    const val ROUTE_WEBSOCKET_TMP_EVENT = "r.websocket.file"
    const val QUEUE_WEBSOCKET_TMP_EVENT = "q.websocket.file"
    const val EXCHANGE_WEBSOCKET_TRANSFER_FANOUT = "e.websocket.transfer.fanout"
    const val ROUTE_WEBSOCKET_TRANSFER_EVENT = "r.websocket.transfer.file"
    const val QUEUE_WEBSOCKET_TRANSFER_EVENT = "q.websocket.transfer.file"
    const val EXCHANGE_WEBSOCKET_SESSION_CLEAR_FANOUT = "e.websocket.session.clear.fanout"
    const val ROUTE_WEBSOCKET_SESSION_CLEAR_EVENT = "r.websocket.session.clear.file"
    const val QUEUE_WEBSOCKET_SESSION_CLEAR_EVENT = "q.websocket.session.clear.file"

    // 工蜂CI请求
    const val EXCHANGE_GITCI_REQUEST_TRIGGER_EVENT = "e.gitci.request.trigger.event"
    const val ROUTE_GITCI_REQUEST_TRIGGER_EVENT = "r.gitci.request.trigger.event"
    const val QUEUE_GITCI_REQUEST_TRIGGER_EVENT = "q.gitci.request.trigger.event"

    // 回调
    const val EXCHANGE_PIPELINE_BUILD_CALL_BACK_FANOUT = "e.engine.pipeline.build.callback.fanout"
    const val QUEUE_PIPELINE_BUILD_STATUS_CHANGE = "e.engine.pipeline.build.callback.change"

    // 蓝盾项目管理
    const val EXCHANGE_PROJECT_CREATE_FANOUT = "e.project.create.exchange.fanout"
    const val EXCHANGE_PROJECT_UPDATE_FANOUT = "e.project.update.exchange.fanout"
    const val EXCHANGE_PROJECT_UPDATE_LOGO_FANOUT = "e.project.update.logo.exchange.fanout"
    const val QUEUE_PROJECT_CREATE_EVENT = "q.project.create.project.queue"
    const val QUEUE_PROJECT_UPDATE_EVENT = "q.project.update.project.queue"
    const val QUEUE_PROJECT_UPDATE_LOGO_EVENT = "q.project.update.logo.project.queue"

    // 蓝盾监控数据上报事件广播
    const val EXCHANGE_ATOM_MONITOR_DATA_REPORT_FANOUT = "e.engine.atom.monitor.data.report.fanout"

    // 蓝盾构建结束后metrics数据上报事件广播
    const val EXCHANGE_BUILD_END_METRICS_DATA_REPORT_FANOUT = "e.engine.build.end.metrics.data.report.fanout"
    // 流水线标签变化metrics数据同步广播
    const val EXCHANGE_PIPELINE_LABEL_CHANGE_METRICS_DATA_SYNC_FANOUT =
        "e.pipeline.label.change.metrics.data.sync.fanout"

    // webhook锁
    const val EXCHANGE_GIT_WEBHOOK_UNLOCK_EVENT = "e.webhook.unlock.event"
    const val ROUTE_GIT_WEBHOOK_UNLOCK_EVENT = "r.webhook.unlock.event"
    const val QUEUE_GIT_WEBHOOK_UNLOCK_EVENT = "q.webhook.unlock.event"

    // 蓝盾管理员
    const val EXCHANGE_AUTH_REFRESH_FANOUT = "e.auth.refresh.exchange.fanout"
    const val ROUTE_AUTH_REFRESH_FANOUT = "r.auth.refresh.exchange.fanout"
    const val QUEUE_AUTH_REFRESH_EVENT = "q.auth.refresh.exchange.queue"

    // 流水线webhook commit记录
    const val EXCHANGE_PIPELINE_BUILD_COMMIT_FINISH_FANOUT = "e.engine.pipeline.build.commits.finish.fanout"

    // 流水线质量红线人工审核时间广播
    const val EXCHANGE_PIPELINE_BUILD_QUALITY_REVIEW_FANOUT = "e.quality.pipeline.build.review.fanout"

    // 质量红线每日数据上报队列
    const val EXCHANGE_QUALITY_DAILY_FANOUT = "e.metrics.quality.daily.exchange.fanout"
    const val ROUTE_QUALITY_DAILY_FANOUT = "r.quality.daily.exchange.fanout"
    const val QUEUE_QUALITY_DAILY_EVENT = "q.metrics.quality.daily.exchange.queue"

    const val ROUTE_PIPELINE_STREAM_ENABLED = "r.engine.pipeline.stream.enabled"
    const val QUEUE_PIPELINE_STREAM_ENABLED = "q.engine.pipeline.stream.enabled"

    // 权限rbac交换机
    const val EXCHANGE_AUTH_RBAC_LISTENER_EXCHANGE = "e.auth.rbac.listener"

    // 权限itsm回调事件
    const val ROUTE_AUTH_ITSM_CALLBACK = "r.auth.itsm.callback"
    const val QUEUE_AUTH_ITSM_CALLBACK = "q.auth.itsm.callback"

    // 权限资源关联用户组创建事件
    const val ROUTE_AUTH_RESOURCE_GROUP_CREATE = "r.auth.resource.group.create"
    const val QUEUE_AUTH_RESOURCE_GROUP_CREATE = "q.auth.resource.group.create"

    // 权限资源关联用户组修改事件
    const val ROUTE_AUTH_RESOURCE_GROUP_MODIFY = "r.auth.resource.group.modify"
    const val QUEUE_AUTH_RESOURCE_GROUP_MODIFY = "q.auth.resource.group.modify"
}
