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

package com.tencent.devops.buildless.utils

const val BUILDLESS_POOL_PREFIX = "buildless-pool"

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
const val ENV_KEY_BK_TAG = "devops_bk_tag"
const val ENV_KEY_BUILD_ID = "devops_build_id"
const val ENV_KEY_AGENT_ID = "devops_agent_id"
const val ENV_KEY_AGENT_SECRET_KEY = "devops_agent_secret_key"
const val ENV_KEY_GATEWAY = "devops_gateway"
const val ENV_DOCKER_HOST_IP = "docker_host_ip"
const val ENV_DOCKER_HOST_PORT = "docker_host_port"
const val ENV_LOG_SAVE_MODE = "devops_log_save_mode"
const val COMMON_DOCKER_SIGN = "devops_slave_model"
const val BK_DISTCC_LOCAL_IP = "BK_DISTCC_LOCAL_IP"
const val ENV_CONTAINER_NAME = "ENV_CONTAINER_NAME"

/**
 * 用于识别dockervm | devcloud |构建机类型
 */
const val ENV_JOB_BUILD_TYPE = "JOB_POOL"

const val ENV_BK_CI_DOCKER_HOST_IP = "BK_CI_DOCKER_HOST_IP" // docker母机IP
const val ENV_BK_CI_DOCKER_HOST_WORKSPACE = "BK_CI_DOCKER_HOST_WORKSPACE" // docker母机工作空间地址
const val ENV_DEFAULT_LOCALE_LANGUAGE = "BK_CI_LOCALE_LANGUAGE"

const val ENTRY_POINT_CMD = "/data/init.sh"

/**
 * bkrepo网关配置
 */
const val ENV_DEVOPS_FILE_GATEWAY = "DEVOPS_FILE_GATEWAY"

/**
 * 蓝盾网关配置
 */
const val ENV_DEVOPS_GATEWAY = "DEVOPS_GATEWAY"
