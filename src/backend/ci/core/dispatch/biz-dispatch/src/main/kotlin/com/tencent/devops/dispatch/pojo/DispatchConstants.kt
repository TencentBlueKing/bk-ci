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

package com.tencent.devops.dispatch.pojo

const val BK_CONSTANT_AGENTS_UPGRADING_OR_TIMED_OUT = "BkConstantAgentsUpgradingOrTimedOut"// 第三方构建机Agent正在升级中 或 排队重试超时，请检查agent（${dispatchType.displayName}）并发任务数设置并稍后重试.
const val BK_THIRD_PARTY_BUILD_MACHINE_STATUS_ERROR = "BkThirdPartyBuildMachineStatusError"// 第三方构建机状态异常，请在环境管理中检查第三方构建机状态(Agent offline)
const val BK_BUILD_MACHINE_UPGRADE_IN_PROGRESS  = "BkBuildMachineUpgradeInProgress"// 构建机升级中，重新调度(Agent is upgrading)
const val BK_BUILD_MACHINE_BUSY = "BkBuildMachineBusy"// 构建机正忙,重新调度(Agent is busy) - ${agent.hostname}/${agent.ip}
const val BK_BUILD_NODE_EMPTY_ERROR = "BkBuildNodeEmptyError"
const val BK_BUILD_ENV_PREPARATION = "BkBuildEnvPreparation"