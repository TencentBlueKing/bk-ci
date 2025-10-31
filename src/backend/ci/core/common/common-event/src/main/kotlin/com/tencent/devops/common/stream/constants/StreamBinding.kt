/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
    const val LOG_ORIGIN_EVENT_DESTINATION = "build.log.origin.event"

    // 日志预处理事件
    const val LOG_STORAGE_EVENT_DESTINATION = "build.log.storage.event"

    // 日志构建状态事件
    const val LOG_STATUS_EVENT_DESTINATION = "build.log.status.event"

    // openapi审计日志预处理事件
    const val OPENAPI_LOG_EVENT = "openapi.log.event"

    // 核心交换机及队列 ====================================
    const val PIPELINE_UPDATE = "engine.pipeline.update"

    const val PIPELINE_CREATE = "engine.pipeline.create"

    const val PIPELINE_DELETE = "engine.pipeline.delete"

    const val PIPELINE_RESTORE = "engine.pipeline.restore"

    const val PIPELINE_TIMER = "engine.pipeline.timer"

    const val PIPELINE_BUILD_START = "engine.pipeline.build.start"

    const val PIPELINE_BUILD_TASK_START = "engine.pipeline.build.task.start"

    const val PIPELINE_BUILD_STAGE = "engine.pipeline.build.stage"

    const val PIPELINE_BUILD_CONTAINER = "engine.pipeline.build.container"

    const val PIPELINE_BUILD_CANCEL = "engine.pipeline.build.cancel"

    const val PIPELINE_BUILD_FINISH = "engine.pipeline.build.finish"

    const val PIPELINE_PAUSE_TASK_EXECUTE = "engine.pipeline.pause.task.execute"

    const val PIPELINE_ARCHIVE = "engine.pipeline.archive"

    const val PIPELINE_BATCH_ARCHIVE = "engine.pipeline.batch.archive"

    const val PIPELINE_BATCH_ARCHIVE_PUBLISH = "engine.pipeline.batch.archive.publish"

    const val PIPELINE_BATCH_ARCHIVE_FINISH = "engine.pipeline.batch.archive.finish"

    // 监控相关的队列
    const val PIPELINE_BUILD_MONITOR = "engine.pipeline.listener.monitor"
    const val PIPELINE_BUILD_HEART_BEAT = "engine.pipeline.build.hb"
    // 构建产生的审核通知类队列
    const val PIPELINE_BUILD_NOTIFY = "engine.pipeline.build.notify"
    // 构建状态Websocket推送解耦
    const val PIPELINE_BUILD_WEBSOCKET = "engine.pipeline.build.websocket"

    const val PIPELINE_BUILD_REVIEW_REMINDER = "engine.pipeline.build.review.reminder.notify"

    // 构建排队广播exchange ====================================
    const val PIPELINE_BUILD_QUEUE_FANOUT = "engine.pipeline.build.queue.fanout"

    // 构建启动广播exchange ====================================
    const val PIPELINE_BUILD_START_FANOUT = "engine.pipeline.build.start.fanout"

    // 构建审核和检查步骤广播exchange ====================================
    const val PIPELINE_BUILD_REVIEW_FANOUT = "engine.pipeline.build.review.fanout"
    const val PIPELINE_BUILD_QUALITY_CHECK_FANOUT = "engine.pipeline.build.quality.check.fanout"

    // 构建结束后续广播exchange ====================================
    const val PIPELINE_BUILD_FINISH_FANOUT = "engine.pipeline.build.finish.fanout"

    // 构建取消后续广播exchange ====================================
    const val PIPELINE_BUILD_CANCEL_FANOUT = "engine.pipeline.build.cancel.fanout"

    // 插件结束后续广播exchange ====================================
    const val PIPELINE_BUILD_TASK_FINISH_FANOUT = "engine.pipeline.build.task.finish"

    // 定时变更广播exchange ====================================
    const val PIPELINE_TIMER_CHANGE_FANOUT = "engine.pipeline.timer.change"

    // 流水线扩展交换器 ====================================
    const val PIPELINE_EXTENDS_FANOUT = "engine.pipeline.extends.fanout"

    // AGENT 构建机消息队列 ====================================
    const val PIPELINE_AGENT_STARTUP = "engine.pipeline.agent.startup"
    const val PIPELINE_AGENT_DEMOTE_STARTUP = "engine.pipeline.agent.startup.demote"
    const val PIPELINE_AGENT_SHUTDOWN = "engine.pipeline.agent.shutdown"
    const val PIPELINE_AGENT_DEMOTE_SHUTDOWN = "engine.pipeline.agent.shutdown.demote"

    // AGENT 构建排队消息队列 ====================================
    const val DISPATCH_AGENT_QUEUE = "dispatch.tp.agent.queue"

    // AGENT 构建监控消息队列
    const val DISPATCH_AGENT_MONITOR = "dispatch.tp.agent.monitor"

    // 无构建环境的Docker构建机启停消息队列 ====================================
    const val BUILD_LESS_AGENT_STARTUP_DISPATCH = "engine.pipeline.bl.agent.dispatch.startup"
    const val BUILD_LESS_AGENT_SHUTDOWN_DISPATCH = "engine.pipeline.bl.agent.dispatch.shutdown"

    // ================================================================================================

    // Git webhook事件回调
    const val ENGINE_GIT_COMMIT_CHECK = "engine.git.commit.check"
    const val ENGINE_GITHUB_PR = "engine.github.pr"

    // SVN代码仓库webhook请求回
    const val SVN_BUILD_REQUEST_EVENT = "engine.pipeline.hook.svn.event"

    // CodeGit代码仓库webhook请求回调
    const val GIT_BUILD_REQUEST_EVENT = "engine.pipeline.hook.git.event"

    // Gitlab代码仓库webhook请求回调
    const val GITLAB_BUILD_REQUEST_EVENT = "engine.pipeline.hook.gitlab.event"

    // Github代码仓库webhook请求回调
    const val GITHUB_BUILD_REQUEST_EVENT = "engine.pipeline.hook.github.event"

    // CodeTGit代码仓库webhook请求回调
    const val TGIT_BUILD_REQUEST_EVENT = "engine.pipeline.hook.tgit.event"

    // P4代码仓库webhook请求回调
    const val P4_BUILD_REQUEST_EVENT = "engine.pipeline.hook.p4.event"

    // P4代码仓库webhook请求回调
    const val REPLAY_BUILD_REQUEST_EVENT = "engine.pipeline.hook.replay.event"

    // scm webhook请求回调
    const val SCM_HOOK_BUILD_REQUEST_EVENT = "engine.pipeline.hook.scm.event"

    // 流水线构建check-run事件
    const val PIPELINE_BUILD_CHECK_RUN = "engine.pipeline.build.check.run"

    // webSocket消息
    const val WEBSOCKET_TMP_FANOUT = "websocket.fanout"
    const val WEBSOCKET_SESSION_CLEAR = "websocket.session.clear.fanout"

    // 回调
    const val PIPELINE_BUILD_CALL_BACK_FANOUT = "engine.pipeline.build.callback.fanout"

    // 蓝盾项目管理
    const val PROJECT_CREATE_FANOUT = "project.create.exchange.fanout"
    const val PROJECT_UPDATE_FANOUT = "project.update.exchange.fanout"
    const val PROJECT_UPDATE_LOGO_FANOUT = "project.update.logo.exchange.fanout"
    const val PROJECT_ENABLE_FANOUT = "project.enable.exchange.fanout"

    // 蓝盾监控数据上报事件广播
    const val ENGINE_ATOM_MONITOR_DATA_REPORT_FANOUT = "engine.atom.monitor.data.report.fanout"

    // 蓝盾构建结束后metrics数据上报事件广播
    const val BUILD_END_METRICS_DATA_REPORT_FANOUT = "metrics.engine.build.end.data.report.fanout"
    // 流水线标签变化metrics数据同步广播
    const val PIPELINE_LABEL_CHANGE_METRICS_DATA_SYNC_FANOUT =
        "metrics.pipeline.label.change.data.sync.fanout"

    // webhook锁
    const val GIT_WEBHOOK_UNLOCK_EVENT = "webhook.unlock.event"

    // 蓝盾管理员
    const val AUTH_MANGER_CHANGE_FANOUT = "auth.manager.change.fanout"
    const val AUTH_MANGER_USER_CHANGE_FANOUT = "auth.manager.user.change.fanout"
    const val AUTH_STRATEGY_UPDATE_FANOUT = "auth.strategy.update.fanout"

    // 流水线webhook commit记录
    const val PIPELINE_BUILD_COMMIT_FINISH_FANOUT = "engine.pipeline.build.commits.finish.fanout"

    // 流水线质量红线人工审核时间广播
    const val PIPELINE_BUILD_QUALITY_REVIEW_FANOUT = "quality.pipeline.build.review.fanout"

    // 度量数据上报队列：质量红线每日数据 + 编译加速每日数据 + 代码检查每日数据
    const val METRICS_STATISTIC_QUALITY_DAILY = "metrics.statistic.quality.daily"
    const val METRICS_STATISTIC_TURBO_DAILY = "metrics.statistic.turbo.daily"
    const val METRICS_STATISTIC_CODE_CHECK_DAILY = "metrics.statistic.code.check.daily"
    const val METRICS_PROJECT_USER_DAILY = "metrics.project.user.daily.fanout"
    const val METRICS_PROJECT_USER_OPERATE_DAILY = "metrics.project.user.operate.daily.fanout"
    const val METRICS_DISPATCH_JOB = "metrics.dispatch.job.daily.exchange.fanout"

    const val PIPELINE_STREAM_ENABLED = "engine.pipeline.stream.enabled"

    // 权限itsm回调事件
    const val AUTH_ITSM_CALLBACK = "auth.itsm.callback"

    // 权限资源关联用户组创建事件
    const val AUTH_RESOURCE_GROUP_CREATE = "auth.resource.group.create"

    // 权限资源关联用户组修改事件
    const val AUTH_RESOURCE_GROUP_MODIFY = "auth.resource.group.modify"

    // 权限项目级权限变更同步事件
    const val AUTH_PROJECT_LEVEL_GROUP_PERMISSIONS_SYNC = "auth.project.level.group.permissions.sync"

    // 数据库分片
    const val SHARDING_ROUTING_RULE_FANOUT = "sharding.routing.rule.exchange.fanout"

    const val PROJECT_COUNT_LOGIN = "project_count_login"

    // pac每条流水线触发事件
    const val PIPELINE_YAML_LISTENER_ENABLE = "pipeline.yaml.listener.enable"
    const val PIPELINE_YAML_LISTENER_DISABLE = "pipeline.yaml.listener.disable"
    const val PIPELINE_YAML_LISTENER_TRIGGER = "pipeline.yaml.listener.trigger"
    const val PIPELINE_YAML_LISTENER_FILE = "pipeline.yaml.listener.file"
}
