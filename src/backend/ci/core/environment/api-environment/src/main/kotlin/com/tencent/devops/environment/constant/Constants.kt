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

package com.tencent.devops.environment.constant

const val THIRD_PARTY_AGENT_HEARTBEAT_INTERVAL = 5L
const val DEFAULT_SYTEM_USER = "devops"
const val BK_PROJECT_NO_ENVIRONMENT = "bkProjectNoEnvironment" // 项目下无环境
const val BK_PROJECT_NO_NODE = "bkProjectNoNode" // 项目下无节点
const val T_NODE_NODE_IP = "nodeIp"
const val T_NODE_HOST_ID = "hostId"
const val T_NODE_NODE_ID = "nodeId"
const val T_NODE_NODE_TYPE = "nodeType"
const val T_NODE_CLOUD_AREA_ID = "cloudAreaId"
const val T_NODE_NODE_STATUS = "nodeStatus"
const val T_NODE_AGENT_VERSION = "agentVersion"
const val T_NODE_AGENT_STATUS = "agentStatus"
const val T_NODE_PROJECT_ID = "projectId"
const val T_NODE_CREATED_USER = "createdUser"
const val T_NODE_OS_TYPE = "osType"
const val T_NODE_OS_NAME = "osName"
const val T_NODE_SERVER_ID = "serverId"
const val T_NODE_OPERATOR = "operator"
const val T_NODE_BAK_OPERATOR = "bakOperator"
const val T_ENV_ENV_ID = "envId"
const val T_ENVIRONMENT_THIRDPARTY_AGENT_NODE_ID = "nodeId"
const val T_ENVIRONMENT_THIRDPARTY_AGENT_MASTER_VERSION = "masterVersion"
const val BATCH_TOKEN_HEADER = "X-DEVOPS-AGENT-INSTALL-TOKEN" // 批量安装agent token的header
