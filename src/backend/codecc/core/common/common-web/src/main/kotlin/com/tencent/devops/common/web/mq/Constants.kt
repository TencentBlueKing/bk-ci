/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
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

package com.tencent.devops.common.web.mq

const val QUEUE_PIPELINE_BUILD = "queue.pipeline.build"
const val QUEUE_PIPELINE_BUILD_NEED_END = "queue.pipeline.build.need.end"
const val EXCHANGE_PIPELINE_BUILD = "exchange.pipeline.build"
const val ROUTE_PIPELINE_BUILD = "route.pipeline.build"
const val ROUTE_PIPELINE_BUILD_NEED_END = "route.pipeline.build.need.end"

const val ROUTE_PIPELINE_FINISH = "route.pipeline.finish"
const val QUEUE_PIPELINE_FINISH = "queue.pipeline.finish"
const val EXCHANGE_PIPELINE_FINISH = "exchange.pipeline.finish"

const val ROUTE_NOTIFY_MESSAGE = "route.notify.message"
const val QUEUE_NOTIFY_MESSAGE = "queue.notify.message"
const val EXCHANGE_NOTIFY_MESSAGE = "exchange.notify.message"

const val ROUTE_PAASCC_PROJECT_CREATE = "route.paascc.project.create"
const val QUEUE_PAASCC_PROJECT_CREATE = "queue.paascc.project.create"
const val EXCHANGE_PAASCC_PROJECT_CREATE = "exchange.paascc.project.create"

const val ROUTE_PAASCC_PROJECT_UPDATE = "route.paascc.project.update"
const val QUEUE_PAASCC_PROJECT_UPDATE = "queue.paascc.project.update"
const val EXCHANGE_PAASCC_PROJECT_UPDATE = "exchange.paascc.project.update"

const val EXCHANGE_PAASCC_PROJECT_UPDATE_LOGO = "exchange.paascc.project.update.logo"
const val ROUTE_PAASCC_PROJECT_UPDATE_LOGO = "route.paascc.project.update.logo"
const val QUEUE_PAASCC_PROJECT_UPDATE_LOGO = "queue.paascc.project.update.logo"

const val ROUTE_GIT_COMMIT_CHECK = "route.git.commit.check"
const val EXCHANGE_GIT_COMMIT_CHECK = "exchange.git.commit.check"
const val QUEUE_GIT_COMMIT_CHECK = "queue.git.commit.check"

const val ROUTE_GITHUB_PR = "route.github.pr"
const val EXCHANGE_GITHUB_PR = "exchange.github.pr"
const val QUEUE_GITHUB_PR = "queue.github.pr"

const val EXCHANGE_TASK_FILTER_PATH = "exchange.task.filter.path"
const val ROUTE_ADD_TASK_FILTER_PATH = "route.add.task.filter.path"
const val QUEUE_ADD_TASK_FILTER_PATH = "queue.add.task.filter.path"

const val ROUTE_DEL_TASK_FILTER_PATH = "route.del.task.filter.path"
const val QUEUE_DEL_TASK_FILTER_PATH = "queue.del.task.filter.path"

const val EXCHANGE_AUTHOR_TRANS = "exchange.author.trans"
const val ROUTE_AUTHOR_TRANS = "route.author.trans"
const val QUEUE_AUTHOR_TRANS = "queue.author.trans"

const val EXCHANGE_OPERATION_HISTORY = "exchange.operation.history"
const val ROUTE_OPERATION_HISTORY = "route.operation.history"
const val QUEUE_OPERATION_HISTORY = "queue.operation.history"

const val EXCHANGE_TASK_CHECKER_CONFIG = "exchange.task.checker.config"
const val ROUTE_IGNORE_CHECKER = "route.ignore.checker.config"
const val QUEUE_IGNORE_CHECKER = "queue.ignore.checker.config"

const val EXCHANGE_ANALYSIS_VERSION = "exchange.analysis.version"
const val ROUTE_ANALYSIS_VERSION = "route.analysis.version"
const val QUEUE_ANALYSIS_VERSION = "queue.analysis.version"

const val EXCHANGE_EXTERNAL_JOB = "exchange.external.job.cluster"
const val QUEUE_EXTERNAL_JOB = "queue.external.job.cluster."

const val EXCHANGE_INTERNAL_JOB = "exchange.internal.job.cluster"
const val ROUTE_INTERNAL_JOB = "route.internal.job.cluster"
const val QUEUE_INTERNAL_JOB = "queue.internal.job.cluster."

const val EXCHANGE_GONGFENG_DELETE_ALL_JOB = "exchange.gongfeng.delete.all.job"
const val QUEUE_GONGFENG_DELETE_ALL_JOB = "queue.gongfeng.delete.all.job."

const val EXCHANGE_GONGFENG_INIT_ALL_JOB = "exchange.gongfeng.init.all.job"
const val QUEUE_GONGFENG_INIT_ALL_JOB = "queue.gongfeng.init.all.job."

const val EXCHANGE_GONGFENG_CODECC_SCAN = "exchange.gongfeng.codecc.scan"
const val ROUTE_GONGFENG_CODECC_SCAN = "route.gongfeng.codecc.scan"
const val QUEUE_GONGFENG_CODECC_SCAN = "queue.gongfeng.codecc.scan"

const val ROUTE_GONGFENG_TRIGGER_PIPELINE = "route.gongfeng.trigger.pipeline"
const val QUEUE_GONGFENG_TRIGGER_PIPELINE = "queue.gongfeng.trigger.pipeline"

const val ROUTE_GONGFENG_ACTIVE_PROJECT = "route.gongfeng.active.project"
const val QUEUE_GONGFENG_ACTIVE_PROJECT = "queue.gongfeng.active.project"

const val ROUTE_GONGFENG_RETRY_TRIGGER = "route.gongfeng.retry.trigger"
const val QUEUE_GONGFENG_RETRY_TRIGGER = "queue.gongfeng.retry.trigger"
const val EXCHANGE_GONGFENG_RETRY_TRIGGER = "exchange.gongfeng.retry.trigger"

const val PREFIX_EXCHANGE_DEFECT_COMMIT = "exchange.defect.commit."
const val PREFIX_ROUTE_DEFECT_COMMIT = "route.defect.commit."
const val PREFIX_QUEUE_DEFECT_COMMIT = "queue.defect.commit."

const val PREFIX_EXCHANGE_OPENSOURCE_DEFECT_COMMIT = "exchange.opensource.defect.commit."
const val PREFIX_ROUTE_OPENSOURCE_DEFECT_COMMIT = "route.opensource.defect.commit."
const val PREFIX_QUEUE_OPENSOURCE_DEFECT_COMMIT = "queue.opensource.defect.commit."

const val EXCHANGE_DEFECT_COMMIT_COVERITY = "exchange.defect.commit.coverity"
const val ROUTE_DEFECT_COMMIT_COVERITY = "route.defect.commit.coverity"
const val QUEUE_DEFECT_COMMIT_COVERITY = "queue.defect.commit.coverity"

const val EXCHANGE_OPENSOURCE_DEFECT_COMMIT_COVERITY = "exchange.opensource.defect.commit.coverity"
const val ROUTE_OPENSOURCE_DEFECT_COMMIT_COVERITY = "route.opensource.defect.commit.coverity"
const val QUEUE_OPENSOURCE_DEFECT_COMMIT_COVERITY = "queue.opensource.defect.commit.coverity"

const val EXCHANGE_DEFECT_COMMIT_KLOCWORK = "exchange.defect.commit.klocwork"
const val ROUTE_DEFECT_COMMIT_KLOCWORK = "route.defect.commit.klocwork"
const val QUEUE_DEFECT_COMMIT_KLOCWORK = "queue.defect.commit.klocwork"

const val EXCHANGE_DEFECT_COMMIT_LINT = "exchange.defect.commit.lint"
const val ROUTE_DEFECT_COMMIT_LINT = "route.defect.commit.lint"
const val QUEUE_DEFECT_COMMIT_LINT = "queue.defect.commit.lint"

const val EXCHANGE_OPENSOURCE_DEFECT_COMMIT_LINT = "exchange.opensource.defect.commit.lint"
const val ROUTE_OPENSOURCE_DEFECT_COMMIT_LINT = "route.opensource.defect.commit.lint"
const val QUEUE_OPENSOURCE_DEFECT_COMMIT_LINT = "queue.opensource.defect.commit.lint"

const val EXCHANGE_DEFECT_COMMIT_CCN = "exchange.defect.commit.ccn"
const val ROUTE_DEFECT_COMMIT_CCN = "route.defect.commit.ccn"
const val QUEUE_DEFECT_COMMIT_CCN = "queue.defect.commit.ccn"

const val EXCHANGE_OPENSOURCE_DEFECT_COMMIT_CCN = "exchange.opensource.defect.commit.ccn"
const val ROUTE_OPENSOURCE_DEFECT_COMMIT_CCN = "route.opensource.defect.commit.ccn"
const val QUEUE_OPENSOURCE_DEFECT_COMMIT_CCN = "queue.opensource.defect.commit.ccn"

const val EXCHANGE_DEFECT_COMMIT_DUPC = "exchange.defect.commit.dupc"
const val ROUTE_DEFECT_COMMIT_DUPC = "route.defect.commit.dupc"
const val QUEUE_DEFECT_COMMIT_DUPC = "queue.defect.commit.dupc"

const val EXCHANGE_OPENSOURCE_DEFECT_COMMIT_DUPC = "exchange.opensource.defect.commit.dupc"
const val ROUTE_OPENSOURCE_DEFECT_COMMIT_DUPC = "route.opensource.defect.commit.dupc"
const val QUEUE_OPENSOURCE_DEFECT_COMMIT_DUPC = "queue.opensource.defect.commit.dupc"

const val EXCHANGE_DEFECT_COMMIT_CLOC = "exchange.defect.commit.cloc"
const val ROUTE_DEFECT_COMMIT_CLOC = "route.defect.commit.cloc"
const val QUEUE_DEFECT_COMMIT_CLOC = "queue.defect.commit.cloc"

const val EXCHANGE_OPENSOURCE_DEFECT_COMMIT_CLOC = "exchange.opensource.defect.commit.cloc"
const val ROUTE_OPENSOURCE_DEFECT_COMMIT_CLOC = "route.opensource.defect.commit.cloc"
const val QUEUE_OPENSOURCE_DEFECT_COMMIT_CLOC = "queue.opensource.defect.commit.cloc"

const val EXCHANGE_DEFECT_COMMIT_PINPOINT = "exchange.defect.commit.pinpoint"
const val ROUTE_DEFECT_COMMIT_PINPOINT = "route.defect.commit.pinpoint"
const val QUEUE_DEFECT_COMMIT_PINPOINT = "queue.defect.commit.pinpoint"

const val EXCHANGE_OPENSOURCE_DEFECT_COMMIT_PINPOINT = "exchange.opensource.defect.commit.pinpoint"
const val ROUTE_OPENSOURCE_DEFECT_COMMIT_PINPOINT = "route.opensource.defect.commit.pinpoint"
const val QUEUE_OPENSOURCE_DEFECT_COMMIT_PINPOINT = "queue.opensource.defect.commit.pinpoint"

const val EXCHANGE_OPENSOURCE_DEFECT_COMMIT_STAT = "exchange.opensource.defect.commit.stat"
const val ROUTE_OPENSOURCE_DEFECT_COMMIT_STAT = "route.opensource.defect.commit.stat"
const val QUEUE_OPENSOURCE_DEFECT_COMMIT_STAT = "queue.opensource.defect.commit.stat"

const val EXCHANGE_DEFECT_COMMIT_LINT_NEW = "exchange.defect.commit.lint.new"
const val ROUTE_DEFECT_COMMIT_LINT_NEW = "route.defect.commit.lint.new"
const val QUEUE_DEFECT_COMMIT_LINT_NEW = "queue.defect.commit.lint.new"

const val EXCHANGE_DEFECT_COMMIT_CCN_NEW = "exchange.defect.commit.ccn.new"
const val ROUTE_DEFECT_COMMIT_CCN_NEW = "route.defect.commit.ccn.new"
const val QUEUE_DEFECT_COMMIT_CCN_NEW = "queue.defect.commit.ccn.new"

const val EXCHANGE_DEFECT_COMMIT_DUPC_NEW = "exchange.defect.commit.dupc.new"
const val ROUTE_DEFECT_COMMIT_DUPC_NEW = "route.defect.commit.dupc.new"
const val QUEUE_DEFECT_COMMIT_DUPC_NEW = "queue.defect.commit.dupc.new"

const val EXCHANGE_DEFECT_COMMIT_CLOC_NEW = "exchange.defect.commit.cloc.new"
const val ROUTE_DEFECT_COMMIT_CLOC_NEW = "route.defect.commit.cloc.new"
const val QUEUE_DEFECT_COMMIT_CLOC_NEW = "queue.defect.commit.cloc.new"

const val EXCHANGE_DEFECT_COMMIT_PINPOINT_NEW = "exchange.defect.commit.pinpoint.new"
const val ROUTE_DEFECT_COMMIT_PINPOINT_NEW = "route.defect.commit.pinpoint.new"
const val QUEUE_DEFECT_COMMIT_PINPOINT_NEW = "queue.defect.commit.pinpoint.new"

const val EXCHANGE_DEFECT_COMMIT_STAT_NEW = "exchange.defect.commit.stat.new"
const val ROUTE_DEFECT_COMMIT_STAT_NEW = "route.defect.commit.stat.new"
const val QUEUE_DEFECT_COMMIT_STAT_NEW = "queue.defect.commit.stat.new"

const val EXCHANGE_DEFECT_COMMIT_METRICS = "exchange.defect.commit.metrics"
const val ROUTE_DEFECT_COMMIT_METRICS = "route.defect.commit.metrics"
const val QUEUE_DEFECT_COMMIT_METRICS = "queue.defect.commit.metrics"

const val EXCHANGE_DEFECT_COMMIT_CLUSTER = "exchange.defect.commit.cluster"
const val ROUTE_DEFECT_COMMIT_CLUSTER = "route.defect.commit.cluster"
const val QUEUE_DEFECT_COMMIT_CLUSTER = "queue.defect.commit.cluster"

const val EXCHANGE_DEFECT_COMMIT_LINT_LARGE = "exchange.defect.commit.lint.large"
const val ROUTE_DEFECT_COMMIT_LINT_LARGE = "route.defect.commit.lint.large"
const val QUEUE_DEFECT_COMMIT_LINT_LARGE = "queue.defect.commit.lint.large"

const val EXCHANGE_DEFECT_COMMIT_CCN_LARGE = "exchange.defect.commit.ccn.large"
const val ROUTE_DEFECT_COMMIT_CCN_LARGE = "route.defect.commit.ccn.large"
const val QUEUE_DEFECT_COMMIT_CCN_LARGE = "queue.defect.commit.ccn.large"

const val EXCHANGE_DEFECT_COMMIT_DUPC_LARGE = "exchange.defect.commit.dupc.large"
const val ROUTE_DEFECT_COMMIT_DUPC_LARGE = "route.defect.commit.dupc.large"
const val QUEUE_DEFECT_COMMIT_DUPC_LARGE = "queue.defect.commit.dupc.large"

const val EXCHANGE_DEFECT_COMMIT_CLOC_LARGE = "exchange.defect.commit.cloc.large"
const val ROUTE_DEFECT_COMMIT_CLOC_LARGE = "route.defect.commit.cloc.large"
const val QUEUE_DEFECT_COMMIT_CLOC_LARGE = "queue.defect.commit.cloc.large"

const val EXCHANGE_DEFECT_COMMIT_PINPOINT_LARGE = "exchange.defect.commit.pinpoint.large"
const val ROUTE_DEFECT_COMMIT_PINPOINT_LARGE = "route.defect.commit.pinpoint.large"
const val QUEUE_DEFECT_COMMIT_PINPOINT_LARGE = "queue.defect.commit.pinpoint.large"

const val EXCHANGE_DEFECT_COMMIT_STAT_LARGE = "exchange.defect.commit.stat.large"
const val ROUTE_DEFECT_COMMIT_STAT_LARGE = "route.defect.commit.stat.large"
const val QUEUE_DEFECT_COMMIT_STAT_LARGE = "queue.defect.commit.stat.large"

const val EXCHANGE_DEFECT_COMMIT_LINT_OPENSOURCE = "exchange.defect.commit.lint.opensource"
const val ROUTE_DEFECT_COMMIT_LINT_OPENSOURCE = "route.defect.commit.lint.opensource"
const val QUEUE_DEFECT_COMMIT_LINT_OPENSOURCE = "queue.defect.commit.lint.opensource"

const val EXCHANGE_DEFECT_COMMIT_CCN_OPENSOURCE = "exchange.defect.commit.ccn.opensource"
const val ROUTE_DEFECT_COMMIT_CCN_OPENSOURCE = "route.defect.commit.ccn.opensource"
const val QUEUE_DEFECT_COMMIT_CCN_OPENSOURCE = "queue.defect.commit.ccn.opensource"

const val EXCHANGE_DEFECT_COMMIT_DUPC_OPENSOURCE = "exchange.defect.commit.dupc.opensource"
const val ROUTE_DEFECT_COMMIT_DUPC_OPENSOURCE = "route.defect.commit.dupc.opensource"
const val QUEUE_DEFECT_COMMIT_DUPC_OPENSOURCE = "queue.defect.commit.dupc.opensource"

const val EXCHANGE_DEFECT_COMMIT_CLOC_OPENSOURCE = "exchange.defect.commit.cloc.opensource"
const val ROUTE_DEFECT_COMMIT_CLOC_OPENSOURCE = "route.defect.commit.cloc.opensource"
const val QUEUE_DEFECT_COMMIT_CLOC_OPENSOURCE = "queue.defect.commit.cloc.opensource"

const val EXCHANGE_DEFECT_COMMIT_PINPOINT_OPENSOURCE = "exchange.defect.commit.pinpoint.opensource"
const val ROUTE_DEFECT_COMMIT_PINPOINT_OPENSOURCE = "route.defect.commit.pinpoint.opensource"
const val QUEUE_DEFECT_COMMIT_PINPOINT_OPENSOURCE = "queue.defect.commit.pinpoint.opensource"

const val EXCHANGE_DEFECT_COMMIT_STAT_OPENSOURCE = "exchange.defect.commit.stat.opensource"
const val ROUTE_DEFECT_COMMIT_STAT_OPENSOURCE = "route.defect.commit.stat.opensource"
const val QUEUE_DEFECT_COMMIT_STAT_OPENSOURCE = "queue.defect.commit.stat.opensource"

const val EXCHANGE_DEFECT_COMMIT_METRICS_OPENSOURCE = "exchange.defect.commit.metrics.opensource"
const val ROUTE_DEFECT_COMMIT_METRICS_OPENSOURCE = "route.defect.commit.metrics.opensource"
const val QUEUE_DEFECT_COMMIT_METRICS_OPENSOURCE = "queue.defect.commit.metrics.opensource"

const val EXCHANGE_DEFECT_COMMIT_CLUSTER_OPENSOURCE = "exchange.defect.commit.cluster.opensource"
const val ROUTE_DEFECT_COMMIT_CLUSTER_OPENSOURCE = "route.defect.commit.cluster.opensource"
const val QUEUE_DEFECT_COMMIT_CLUSTER_OPENSOURCE = "queue.defect.commit.cluster.opensource"

const val EXCHANGE_DEFECT_COMMIT_SUPER_LARGE = "exchange.defect.commit.super.large"
const val ROUTE_DEFECT_COMMIT_SUPER_LARGE = "route.defect.commit.super.large"
const val QUEUE_DEFECT_COMMIT_SUPER_LARGE = "queue.defect.commit.super.large"

const val EXCHANGE_ANALYZE_DISPATCH = "exchange.analyze.schedule"
const val ROUTE_ANALYZE_DISPATCH = "route.analyze.schedule"
const val QUEUE_ANALYZE_DISPATCH = "queue.analyze.schedule"

const val EXCHANGE_ANALYZE_DISPATCH_OPENSOURCE = "exchange.analyze.schedule.opensource"
const val ROUTE_ANALYZE_DISPATCH_OPENSOURCE = "route.analyze.schedule.opensource"
const val QUEUE_ANALYZE_DISPATCH_OPENSOURCE = "queue.analyze.schedule.opensource"

const val EXCHANGE_CHECK_THREAD_ALIVE = "exchange.check.thread.alive"
const val ROUTE_CHECK_THREAD_ALIVE = "route.check.thread.alive"
const val QUEUE_CHECK_THREAD_ALIVE = "queue.check.thread.alive"


const val EXCHANGE_CODECC_GENERAL_NOTIFY = "exchange.codecc.general.notify"
const val ROUTE_CODECC_EMAIL_NOTIFY = "route.codecc.email.notify"
const val QUEUE_CODECC_EMAIL_NOTIFY = "queue.codecc.email.notify"

const val EXCHANGE_REGISTER_KW_PROJECT = "exchange.register.kw.project"
const val ROUTE_REGISTER_KW_PROJECT = "route.register.kw.project"
const val QUEUE_REGISTER_KW_PROJECT = "queue.register.kw.project"

const val ROUTE_CODECC_RTX_NOTIFY = "route.codecc.rtx.notify"
const val QUEUE_CODECC_RTX_NOTIFY = "queue.codecc.rtx.notify"

const val ROUTE_CODECC_BKPLUGINEMAIL_NOTIFY = "route.codecc.bkpluginemail.notify"
const val QUEUE_CODECC_BKPLUGINEMAIL_NOTIFY = "queue.codecc.bkpluginemail.notify"

const val ROUTE_CODECC_BKPLUGINWECHAT_NOTIFY = "route.codecc.bkpluginwechat.notify"
const val QUEUE_CODECC_BKPLUGINWECHAT_NOTIFY = "queue.codecc.bkpluginwechat.notify"


const val EXCHANGE_KAFKA_DATA_PLATFORM = "exchange.kafka.data.platform"
const val ROUTE_KAFKA_DATA_TASK_DETAIL = "route.kafka.data.task.detail"
const val QUEUE_KAFKA_DATA_TASK_DETAIL = "queue.kafka.data.task.detail"

const val ROUTE_KAFKA_DATA_GONGFENG_PROJECT = "route.kafka.data.gongfeng.project"
const val QUEUE_KAFKA_DATA_GONGFENG_PROJECT = "queue.kafka.data.gongfeng.project"

const val ROUTE_KAFKA_DATA_COMMON_STATISTIC = "route.kafka.data.common.statistic"
const val QUEUE_KAFKA_DATA_COMMON_STATISTIC = "queue.kafka.data.common.statistic"

const val ROUTE_KAFKA_DATA_LINT_STATISTIC = "route.kafka.data.lint.statistic"
const val QUEUE_KAFKA_DATA_LINT_STATISTIC = "queue.kafka.data.lint.statistic"

const val ROUTE_KAFKA_DATA_CCN_STATISTIC = "route.kafka.data.ccn.statistic"
const val QUEUE_KAFKA_DATA_CCN_STATISTIC = "queue.kafka.data.ccn.statistic"

const val ROUTE_KAFKA_DATA_DUPC_STATISTIC = "route.kafka.data.dupc.statistic"
const val QUEUE_KAFKA_DATA_DUPC_STATISTIC = "queue.kafka.data.dupc.statistic"

const val ROUTE_KAFKA_DATA_ACTIVE_PROJECT = "route.kafka.data.active.project"
const val QUEUE_KAFKA_DATA_ACTIVE_PROJECT = "queue.kafka.data.active.project"

const val ROUTE_KAFKA_DATA_CLOC_DEFECT = "route.kafka.data.cloc.defect"
const val QUEUE_KAFKA_DATA_CLOC_DEFECT = "queue.kafka.data.cloc.defect"


const val EXCHANGE_TASKLOG_DEFECT_WEBSOCKET = "exchange.tasklog.defect.websocket"
const val ROUTE_TASKLOG_DEFECT_WEBSOCKET = "route.tasklog.defect.websocket"
const val QUEUE_TASKLOG_DEFECT_WEBSOCKET = "queue.tasklog.defect.websocket."


const val ROUTE_KAFKA_DATA_TRIGGER_TASK = "route.kafka.data.trigger.task"
const val QUEUE_KAFKA_DATA_TRIGGER_TASK = "queue.kafka.data.trigger.task"


const val EXCHANGE_EXPIRED_TASK_STATUS = "exchange.expired.task.status"
const val ROUTE_EXPIRED_TASK_STATUS = "route.expired.task.status"
const val QUEUE_EXPIRED_TASK_STATUS = "queue.expired.task.status"

const val EXCHANGE_REFRESH_CHECKERSET_USAGE = "exchange.refresh.checkerset.usage"
const val ROUTE_REFRESH_CHECKERSET_USAGE = "route.refresh.checkerset.usage"
const val QUEUE_REFRESH_CHECKERSET_USAGE = "queue.refresh.checkerset.usage"

const val EXCHANGE_GONGFENG_STAT_SYNC = "exchange.gongfeng.stat.sync"
const val ROUTE_GONGFENG_STAT_SYNC = "route.gongfeng.stat.sync"
const val QUEUE_GONGFENG_STAT_SYNC = "queue.gongfeng.stat.sync"

const val EXCHANGE_GONGFENG_STAT_SYNC_NEW = "exchange.gongfeng.stat.sync.new"
const val ROUTE_GONGFENG_STAT_SYNC_NEW = "route.gongfeng.stat.sync.new"
const val QUEUE_GONGFENG_STAT_SYNC_NEW = "queue.gongfeng.stat.sync.new"

const val EXCHANGE_REFRESH_TOOLMETA_CACHE = "exchange.refresh.toolmeta.cache"
const val QUEUE_REFRESH_TOOLMETA_CACHE = "queue.refresh.toolmeta.cache"

const val EXCHANGE_CLUSTER_ALLOCATION = "exchange.cluster.allocation"
const val ROUTE_CLUSTER_ALLOCATION = "route.cluster.allocation"
const val QUEUE_CLUSTER_ALLOCATION = "queue.cluster.allocation"
const val QUEUE_REPLY_CLUSTER_ALLOCATION = "queue.reply.cluster.allocation"

const val EXCHANGE_CLUSTER_ALLOCATION_OPENSOURCE = "exchange.cluster.allocation.opensource"
const val ROUTE_CLUSTER_ALLOCATION_OPENSOURCE = "route.cluster.allocation.opensource"
const val QUEUE_CLUSTER_ALLOCATION_OPENSOURCE = "queue.cluster.allocation.opensource"
const val QUEUE_REPLY_CLUSTER_ALLOCATION_OPENSOURCE = "queue.reply.cluster.allocation.opensource"

const val EXCHANGE_CUSTOM_PIPELINE_TRIGGER = "exchange.custom.pipeline.trigger"
const val ROUTE_CUSTOM_PIPELINE_TRIGGER = "route.custom.pipeline.trigger"
const val QUEUE_CUSTOM_PIPELINE_TRIGGER = "queue.custom.pipeline.trigger"

const val EXCHANGE_TASK_REFRESH_ORG = "exchange.task.refresh.org"
const val ROUTE_TASK_REFRESH_ORG = "route.task.refresh.org"
const val QUEUE_TASK_REFRESH_ORG = "queue.task.refresh.org"

const val EXCHANGE_LINT_DEFECT_MIGRATION = "exchange.lint.defect.migration"
const val ROUTE_LINT_DEFECT_MIGRATION = "route.lint.defect.migration"
const val QUEUE_LINT_DEFECT_MIGRATION = "queue.lint.defect.migration"

const val PREFIX_EXCHANGE_FAST_INCREMENT = "exchange.fast.increment."
const val PREFIX_ROUTE_FAST_INCREMENT = "route.fast.increment."

const val EXCHANGE_FAST_INCREMENT_LINT = "exchange.fast.increment.lint"
const val ROUTE_FAST_INCREMENT_LINT = "route.fast.increment.lint"
const val QUEUE_FAST_INCREMENT_LINT = "queue.fast.increment.lint"

const val EXCHANGE_FAST_INCREMENT_CCN = "exchange.fast.increment.ccn"
const val ROUTE_FAST_INCREMENT_CCN = "route.fast.increment.ccn"
const val QUEUE_FAST_INCREMENT_CCN = "queue.fast.increment.ccn"

const val EXCHANGE_FAST_INCREMENT_DUPC = "exchange.fast.increment.dupc"
const val ROUTE_FAST_INCREMENT_DUPC = "route.fast.increment.dupc"
const val QUEUE_FAST_INCREMENT_DUPC = "queue.fast.increment.dupc"

const val EXCHANGE_FAST_INCREMENT_CLOC = "exchange.fast.increment.cloc"
const val ROUTE_FAST_INCREMENT_CLOC = "route.fast.increment.cloc"
const val QUEUE_FAST_INCREMENT_CLOC = "queue.fast.increment.cloc"

const val EXCHANGE_FAST_INCREMENT_STAT = "exchange.fast.increment.stat"
const val ROUTE_FAST_INCREMENT_STAT = "route.fast.increment.stat"
const val QUEUE_FAST_INCREMENT_STAT = "queue.fast.increment.stat"

const val EXCHANGE_FAST_INCREMENT_PINPOINT = "exchange.fast.increment.pinpoint"
const val ROUTE_FAST_INCREMENT_PINPOINT = "route.fast.increment.pinpoint"
const val QUEUE_FAST_INCREMENT_PINPOINT = "queue.fast.increment.pinpoint"

const val EXCHANGE_FAST_INCREMENT_COVERITY = "exchange.fast.increment.coverity"
const val ROUTE_FAST_INCREMENT_COVERITY = "route.fast.increment.coverity"
const val QUEUE_FAST_INCREMENT_COVERITY = "queue.fast.increment.coverity"

const val EXCHANGE_FAST_INCREMENT_KLOCWORK = "exchange.fast.increment.klocwork"
const val ROUTE_FAST_INCREMENT_KLOCWORK = "route.fast.increment.klocwork"
const val QUEUE_FAST_INCREMENT_KLOCWORK = "queue.fast.increment.klocwork"

const val EXCHANGE_FAST_INCREMENT_LINT_OPENSOURCE = "exchange.fast.increment.lint.opensource"
const val ROUTE_FAST_INCREMENT_LINT_OPENSOURCE = "route.fast.increment.lint.opensource"
const val QUEUE_FAST_INCREMENT_LINT_OPENSOURCE = "queue.fast.increment.lint.opensource"

const val EXCHANGE_FAST_INCREMENT_CCN_OPENSOURCE = "exchange.fast.increment.ccn.opensource"
const val ROUTE_FAST_INCREMENT_CCN_OPENSOURCE = "route.fast.increment.ccn.opensource"
const val QUEUE_FAST_INCREMENT_CCN_OPENSOURCE = "queue.fast.increment.ccn.opensource"

const val EXCHANGE_FAST_INCREMENT_DUPC_OPENSOURCE = "exchange.fast.increment.dupc.opensource"
const val ROUTE_FAST_INCREMENT_DUPC_OPENSOURCE = "route.fast.increment.dupc.opensource"
const val QUEUE_FAST_INCREMENT_DUPC_OPENSOURCE = "queue.fast.increment.dupc.opensource"

const val EXCHANGE_FAST_INCREMENT_CLOC_OPENSOURCE = "exchange.fast.increment.cloc.opensource"
const val ROUTE_FAST_INCREMENT_CLOC_OPENSOURCE = "route.fast.increment.cloc.opensource"
const val QUEUE_FAST_INCREMENT_CLOC_OPENSOURCE = "queue.fast.increment.cloc.opensource"

const val EXCHANGE_FAST_INCREMENT_STAT_OPENSOURCE = "exchange.fast.increment.stat.opensource"
const val ROUTE_FAST_INCREMENT_STAT_OPENSOURCE = "route.fast.increment.stat.opensource"
const val QUEUE_FAST_INCREMENT_STAT_OPENSOURCE = "queue.fast.increment.stat.opensource"

const val EXCHANGE_FAST_INCREMENT_PINPOINT_OPENSOURCE = "exchange.fast.increment.pinpoint.opensource"
const val ROUTE_FAST_INCREMENT_PINPOINT_OPENSOURCE = "route.fast.increment.pinpoint.opensource"
const val QUEUE_FAST_INCREMENT_PINPOINT_OPENSOURCE = "queue.fast.increment.pinpoint.opensource"

const val EXCHANGE_FAST_INCREMENT_COVERITY_OPENSOURCE = "exchange.fast.increment.coverity.opensource"
const val ROUTE_FAST_INCREMENT_COVERITY_OPENSOURCE = "route.fast.increment.coverity.opensource"
const val QUEUE_FAST_INCREMENT_COVERITY_OPENSOURCE = "queue.fast.increment.coverity.opensource"

const val EXCHANGE_FAST_INCREMENT_KLOCWORK_OPENSOURCE = "exchange.fast.increment.klocwork.opensource"
const val ROUTE_FAST_INCREMENT_KLOCWORK_OPENSOURCE = "route.fast.increment.klocwork.opensource"
const val QUEUE_FAST_INCREMENT_KLOCWORK_OPENSOURCE = "queue.fast.increment.klocwork.opensource"

const val EXCHANGE_TOOL_REFRESH_FOLLOWSTATUS = "exchange.tool.refresh.followstatus"
const val ROUTE_TOOL_REFRESH_FOLLOWSTATUS = "route.tool.refresh.followstatus"
const val QUEUE_TOOL_REFRESH_FOLLOWSTATUS = "queue.tool.refresh.followstatus"

const val EXCHANGE_SCORING_OPENSOURCE = "exchange.scoring.opensource"
const val ROUTE_SCORING_OPENSOURCE = "route.scoring.opensource"
const val QUEUE_SCORING_OPENSOURCE = "queue.scoring.opensource"

const val EXCHANGE_ATOM_MONITOR_DATA_REPORT_FANOUT = "e.engine.atom.monitor.data.report.fanout"
const val QUEUE_CODECC_OPENSOURCE_FAIL_DATA_REPORT = "queue.codecc.opensource.fail.data.report"

const val EXCHANGE_COV_DEFECT_DETAIL_SYNC = "exchange.cov.defect.detail.sync"
const val ROUTE_COV_DEFECT_DETAIL_SYNC = "route.cov.defect.detail.sync"
const val QUEUE_COV_DEFECT_DETAIL_SYNC = "queue.cov.defect.detail.sync"

const val EXCHANGE_COV_DEFECT_DETAIL_SYNC_OPENSOURCE = "exchange.cov.defect.detail.sync.opensource"
const val ROUTE_COV_DEFECT_DETAIL_SYNC_OPENSOURCE = "route.cov.defect.detail.sync.opensource"
const val QUEUE_COV_DEFECT_DETAIL_SYNC_OPENSOURCE = "queue.cov.defect.detail.sync.opensource"

const val EXCHANGE_CLOSE_DEFECT_STATISTIC = "exchange.close.defect.statistic"
const val ROUTE_CLOSE_DEFECT_STATISTIC = "route.close.defect.statistic"
const val QUEUE_CLOSE_DEFECT_STATISTIC = "queue.close.defect.statistic"

const val EXCHANGE_CLOSE_DEFECT_STATISTIC_LINT = "exchange.close.defect.statistic.lint"
const val ROUTE_CLOSE_DEFECT_STATISTIC_LINT = "route.close.defect.statistic.lint"
const val QUEUE_CLOSE_DEFECT_STATISTIC_LINT = "queue.close.defect.statistic.lint"

const val EXCHANGE_CLOSE_DEFECT_STATISTIC_CCN = "exchange.close.defect.statistic.ccn"
const val ROUTE_CLOSE_DEFECT_STATISTIC_CCN = "route.close.defect.statistic.ccn"
const val QUEUE_CLOSE_DEFECT_STATISTIC_CCN = "queue.close.defect.statistic.ccn"

const val EXCHANGE_CLOSE_DEFECT_STATISTIC_OPENSOURCE = "exchange.close.defect.statistic.opensource"
const val ROUTE_CLOSE_DEFECT_STATISTIC_OPENSOURCE = "route.close.defect.statistic.opensource"
const val QUEUE_CLOSE_DEFECT_STATISTIC_OPENSOURCE = "queue.close.defect.statistic.opensource"

const val EXCHANGE_CLOSE_DEFECT_STATISTIC_LINT_OPENSOURCE = "exchange.close.defect.statistic.lint.opensource"
const val ROUTE_CLOSE_DEFECT_STATISTIC_LINT_OPENSOURCE = "route.close.defect.statistic.lint.opensource"
const val QUEUE_CLOSE_DEFECT_STATISTIC_LINT_OPENSOURCE = "queue.close.defect.statistic.lint.opensource"

const val EXCHANGE_CLOSE_DEFECT_STATISTIC_CCN_OPENSOURCE = "exchange.close.defect.statistic.ccn.opensource"
const val ROUTE_CLOSE_DEFECT_STATISTIC_CCN_OPENSOURCE = "route.close.defect.statistic.ccn.opensource"
const val QUEUE_CLOSE_DEFECT_STATISTIC_CCN_OPENSOURCE = "queue.close.defect.statistic.ccn.opensource"

const val EXCHANGE_USER_LOG_INFO_STAT = "exchange.user.log.info.stat"
const val ROUTE_USER_LOG_INFO_STAT = "route.user.log.info.stat"
const val QUEUE_USER_LOG_INFO_STAT = "queue.user.log.info.stat"

const val EXCHANGE_CHECKER_DEFECT_STAT = "exchange.checker.defect.stat"
const val ROUTE_CHECKER_DEFECT_STAT = "route.checker.defect.stat"
const val QUEUE_CHECKER_DEFECT_STAT = "queue.checker.defect.stat"

const val EXCHANGE_ACTIVE_STAT = "exchange.active.stat"
const val ROUTE_ACTIVE_STAT = "route.active.stat"
const val QUEUE_ACTIVE_STAT = "queue.active.stat"

const val EXCHANGE_TASK_PERSONAL = "exchange.task.personal"
const val ROUTE_TASK_PERSONAL = "route.task.personal"
const val QUEUE_TASK_PERSONAL = "queue.task.personal"

const val EXCHANGE_GRAY_TASK_POOL = "exchange.gray.task.pool"
const val ROUTE_GRAY_TASK_POOL_CREATE = "route.gray.task.pool.create"
const val QUEUE_GRAY_TASK_POOL_CREATE = "queue.gray.task.pool.create"
const val ROUTE_GRAY_TASK_POOL_TRIGGER = "route.gray.task.pool.trigger"
const val QUEUE_GRAY_TASK_POOL_TRIGGER = "queue.gray.task.pool.trigger"

const val EXCHANGE_CODE_REPO_STAT = "exchange.code.repo.stat"
const val ROUTE_CODE_REPO_STAT = "route.code.repo.stat"
const val QUEUE_CODE_REPO_STAT = "queue.code.repo.stat"
