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

package com.tencent.devops.worker.common

const val BUILD_ID = "devops.build.id"

const val BUILD_TYPE = "build.type"

const val WORKSPACE_ENV = "WORKSPACE"

const val COMMON_ENV_CONTEXT = "common_env"

const val WORKSPACE_CONTEXT = "ci.workspace"

const val CI_TOKEN_CONTEXT = "ci.token"

const val JOB_OS_CONTEXT = "job.os"

const val SLAVE_AGENT_START_FILE = "devops.slave.agent.start.file"

const val SLAVE_AGENT_PREPARE_START_FILE = "devops.slave.agent.prepare.start.file"

const val AGENT_ERROR_MSG_FILE = "devops.agent.error.file"

const val CLEAN_WORKSPACE = "DEVOPS_CLEAN_WORKSPACE"

const val JAVA_PATH_ENV = "bk_java_path"

const val NODEJS_PATH_ENV = "bk_nodejs_path"

const val LOG_DEBUG_FLAG = "##[debug]"

const val LOG_ERROR_FLAG = "##[error]"

const val LOG_WARN_FLAG = "##[warning]"

const val LOG_SUBTAG_FLAG = "##subTag##"

const val LOG_SUBTAG_FINISH_FLAG = "##subTagFinish##"

const val LOG_UPLOAD_BUFFER_SIZE = 200

const val LOG_MESSAGE_LENGTH_LIMIT = 16 * 1024 // 16KB

const val LOG_TASK_LINE_LIMIT = 1000000

const val LOG_FILE_LENGTH_LIMIT = 1073741824 // 1 GB = 1073741824 Byte

val PIPELINE_SCRIPT_ATOM_CODE = listOf("PipelineScriptDev", "PipelineScriptTest", "PipelineScript")

const val BK_CI_ATOM_EXECUTE_ENV_PATH = "BK_CI_ATOM_EXECUTE_ENV_PATH"
