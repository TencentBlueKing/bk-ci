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

package com.tencent.devops.dispatch.constants

const val QUEUE_BUILD = "queue_build"
const val QUEUE_BUILD_NEED_END = "queue_build_need_end"
const val EXCHANGE_BUILD = "exchange_build"
const val ROUTE_BUILD = "build"
const val ROUTE_BUILD_NEED_END = "build_need_end"

const val EXCHANGE_BUILD_ABORT = "exchange_build_abort"
const val ROUTE_BUILD_ABORT = "route_build_abort"
const val QUEUE_BUILD_ABORT = "queue_build_abort"

const val QUEUE_TASK_BEGIN = "queue_dispatch_vm_task_begin"
const val QUEUE_TASK_END = "queue_dispatch_vm_task_end"
const val QUEUE_TASK_NEDD_END = "queue_dispatch_vm_task_need_end"
const val EXCHANGE_TASK = "exchange_vm_task"
const val ROUTE_TASK_BEGIN = "dispatch_vm_task_begin"
const val ROUTE_TASK_END = "dispatch_vm_task_end"
const val ROUTE_TASK_NEDD_END = "dispatch_vm_task_need_end"
const val ENV_PUBLIC_HOST_MAX_ATOM_FILE_CACHE_SIZE = "PUBLIC_HOST_MAX_ATOM_FILE_CACHE_SIZE"
const val ENV_THIRD_HOST_MAX_ATOM_FILE_CACHE_SIZE = "THIRD_HOST_MAX_ATOM_FILE_CACHE_SIZE"

// |Docker构建|最大并行构建量(maximum parallelism)/当前正在运行构建数量(Running):
const val BK_DOCKER_BUILD_VOLUME = "bkDockerBuildVolume"
// |最大并行构建量(maximum parallelism)/当前正在运行构建数量(Running):
const val BK_MAXIMUM_PARALLELISM = "bkMaximumParallelism"
const val BK_UNLIMITED = "bkUnlimited" // 无限制(unlimited), 注意负载(Attention)
const val BK_HEARTBEAT_TIME = "bkHeartbeatTime" // 构建机最近心跳时间（heartbeat Time)
const val BK_DOCKER_WAS_RECENTLY_BUILT = "bkDockerWasRecentlyBuilt" // |Docker构建|最近{0}次运行中的构建:
const val BK_WAS_RECENTLY_BUILT = "bkWasRecentlyBuilt" // |最近{0}次运行中的构建:
const val BK_TASK_FETCHING_TIMEOUT = "bkTaskFetchingTimeout" // 任务领取超过{0} 分钟没有启动, 可能存在异常，开始重置
const val BK_SCHEDULING_SELECTED_AGENT = "bkSchedulingSelectedAgent" // 调度构建机(Scheduling selected Agent): {0}/{1}
const val BK_SEARCHING_AGENT = "bkSearchingAgent" // 开始查找最近使用过并且当前没有任何任务的空闲构建机...
const val BK_MAX_BUILD_SEARCHING_AGENT = "bkMaxBuildSearchingAgent" // 查找最近使用过并且未达到最大构建数的构建机...
const val BK_SEARCHING_AGENT_MOST_IDLE = "bkSearchingAgentMostIdle" // 开始查找没有任何任务的空闲构建机...
const val BK_SEARCHING_AGENT_PARALLEL_AVAILABLE = "bkSearchingAgentParallelAvailable" // 开始查找当前构建任务还没到达最大并行数构建机...
const val BK_NO_AGENT_AVAILABLE = "bkNoAgentAvailable" // 没有可用Agent，等待Agent释放...
const val BK_ENV_BUSY = "bkEnvBusy" // 构建环境并发保护，稍后重试...
const val BK_QUEUE_TIMEOUT_MINUTES = "bkQueueTimeoutMinutes" //  构建环境无可分配构建机，等待超时（queue-timeout-minutes={0}）
const val BK_AGENT_IS_BUSY = "bkAgentIsBusy" // 构建机繁忙，继续重试(Agent is busy)
const val BK_BUILD_AGENT_DETAIL_LINK_ERROR = "bkBuildAgentDetailLinkError" // 构建机Agent详情链接
const val BK_ENV_WORKER_ERROR_IGNORE = "bkEnvWorkerErrorIgnore" // 构建机环境中{0}节点启动构建进程失败，自动切换其他节点重试
const val AGENT_REUSE_MUTEX_REDISPATCH = "agentReuseMuteXRedispatch" // 构建机复用互斥，节点 {0} 已被 {1} 构建使用，重新调度
// 构建机复用互斥，等待被依赖的节点 {0} 调度到具体节点后再进行复用调度
const val AGENT_REUSE_MUTEX_WAIT_REUSED_ENV = "agentReuseMuteXWaitReusedEnv"
const val BK_ENV_NODE_DISABLE = "bkEnvNodeDisable"
const val BK_THIRD_JOB_ENV_CURR = "bkThirdJobEnvCurr" // 当前环境下所有构建机并发{0}已经超过配置的{1},排队{2}分钟
const val BK_THIRD_JOB_NODE_CURR = "bkThirdJobNodeCurr" // 当前环境下所有节点运行任务都超过了配置的{0},排队{1}分钟
// 构建机复用互斥，节点 {0} 已被 {1} 构建使用，剩余可调度空间不足，重新调度
const val AGENT_REUSE_MUTEX_RESERVE_REDISPATCH = "agentReuseMutexReserveRedispatch"
// 构建环境调度结束，已选取节点 {0}
const val BK_ENV_DISPATCH_AGENT = "bkEnvDispatchAgent"
// 尝试下发任务至节点 {0}
const val TRY_AGENT_DISPATCH = "tryAgentDispatch"