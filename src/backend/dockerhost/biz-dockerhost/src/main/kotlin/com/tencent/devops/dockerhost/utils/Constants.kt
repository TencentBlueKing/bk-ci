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

package com.tencent.devops.dockerhost.utils

const val BUILD_ID_DEFAULT = "0000000000000000000000000000000"
const val VM_SEQ_ID_DEFAULT = "1"
const val VM_NAME_DEFAULT = "vmName"

const val SLAVE_MODEL = "devops.slave.model"
const val SLAVE_MODEL_WORKER = "worker"
const val SLAVE_MODEL_AGENT = "agent"
const val SLAVE_MODEL_PLUGIN_AGENT = "pluginAgent"

const val SLAVE_AGENT_ROLE = "devops.slave.agent.role"
const val SLAVE_AGENT_ROLE_MASTER = "devops.slave.agent.role.master"
const val SLAVE_AGENT_ROLE_SLAVE = "devops.slave.agent.role.slave"
const val SLAVE_AGENT_START_FILE = "devops.slave.agent.start.file"

const val SLAVE_BUILD_TYPE = "build.type"

const val WORKSPACE_ENV = "WORKSPACE"

const val ENV_KEY_DISTCC = "DISTCC_HOSTS"
const val ENV_KEY_PROJECT_ID = "devops_project_id"
const val ENV_KEY_AGENT_ID = "devops_agent_id"
const val ENV_KEY_AGENT_SECRET_KEY = "devops_agent_secret_key"
const val ENV_KEY_GATEWAY = "devops_gateway"
const val ENV_DOCKER_HOST_IP = "docker_host_ip"
const val ENV_DOCKER_HOST_PORT = "docker_host_port"
const val BK_DISTCC_LOCAL_IP = "BK_DISTCC_LOCAL_IP"

const val ENTRY_POINT_CMD = "/data/init.sh"
