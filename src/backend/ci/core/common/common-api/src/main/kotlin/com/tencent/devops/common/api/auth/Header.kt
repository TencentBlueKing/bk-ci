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

package com.tencent.devops.common.api.auth

/**
 *
 * Powered By Tencent
 */

const val AUTH_HEADER_USER_ID: String = "X-DEVOPS-UID"
const val AUTH_HEADER_USER_ID_DEFAULT_VALUE: String = "admin"
const val AUTH_HEADER_BUILD_ID: String = "X-DEVOPS-BUILD-ID"
const val AUTH_HEADER_VM_SEQ_ID: String = "X-DEVOPS-VM-SID"
const val AUTH_HEADER_VM_NAME: String = "X-DEVOPS-VM-NAME"
const val AUTH_HEADER_PROJECT_ID: String = "X-DEVOPS-PROJECT-ID"
const val AUTH_HEADER_AGENT_SECRET_KEY: String = "X-DEVOPS-AGENT-SECRET-KEY"
const val AUTH_HEADER_AGENT_ID: String = "X-DEVOPS-AGENT-ID"
const val AUTH_HEADER_PIPELINE_ID: String = "X-DEVOPS-PIPELINE-ID"
const val AUTH_HEADER_REGION: String = "X-DEVOPS-REGION"

const val AUTH_HEADER_DEVOPS_APP_CODE: String = "X-DEVOPS-APP-CODE"
const val AUTH_HEADER_DEVOPS_APP_SECRET: String = "X-DEVOPS-APP-SECRET"
const val AUTH_HEADER_DEVOPS_BUILD_TYPE: String = "X-DEVOPS-BUILD-TYPE"
const val AUTH_HEADER_DEVOPS_USER_ID = AUTH_HEADER_USER_ID
const val AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE: String = "admin"

const val AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE: String = "bkci"

const val AUTH_HEADER_DEVOPS_BG_ID = "X-DEVOPS-BG-ID"
const val AUTH_HEADER_DEVOPS_DEPT_ID = "X-DEVOPS-DEPT-ID"
const val AUTH_HEADER_DEVOPS_CENTER_ID = "X-DEVOPS-CENTER-ID"
const val AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE = "X-DEVOPS-ORGANIZATION-TYPE"
const val AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG = "BG"
const val AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_DEPARTMENT = "DEPARTMENT"
const val AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_CENTER = "CENTER"
const val AUTH_HEADER_DEVOPS_ORGANIZATION_ID = "X-DEVOPS-ORGANIZATION-ID"
const val AUTH_HEADER_DEVOPS_ORGANIZATION_NAME = "X-DEVOPS-ORGANIZATION-NAME"

const val AUTH_HEADER_DEVOPS_PROJECT_ID: String = "X-DEVOPS-PROJECT-ID"
const val AUTH_HEADER_DEVOPS_PIPELINE_ID: String = "X-DEVOPS-PIPELINE-ID"
const val AUTH_HEADER_DEVOPS_BUILD_ID: String = "X-DEVOPS-BUILD-ID"
const val AUTH_HEADER_DEVOPS_VM_SEQ_ID: String = "X-DEVOPS-VM-SID"
const val AUTH_HEADER_DEVOPS_VM_NAME: String = "X-DEVOPS-VM-NAME"

const val AUTH_HEADER_DEVOPS_REAL_IP: String = "X-DEVOPS-REAL-IP"

const val AUTH_HEADER_DEVOPS_AGENT_ID: String = "X-DEVOPS-AGENT-ID"
const val AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY: String = "X-DEVOPS-AGENT-SECRET-KEY"

const val AUTH_HEADER_DEVOPS_BK_TOKEN: String = "X-DEVOPS-BK-TOKEN"
const val AUTH_HEADER_DEVOPS_ACCESS_TOKEN: String = "X-DEVOPS-ACCESS-TOKEN"
const val AUTH_HEADER_DEVOPS_BK_TICKET: String = "X-DEVOPS-BK-TOKEN"
const val AUTH_HEADER_DEVOPS_USER_CHINESE_NAME = "X-DEVOPS-CHINESE-NAME"
const val AUTH_HEADER_DEVOPS_USER_PAAS_ID = "X-DEVOPS-PAAS-USER-ID"

const val AUTH_HEADER_DEVOPS_ORIGINAL_HOST = "X-DEVOPS-ORIGINAL-HOST"

const val AUTH_HEADER_DEVOPS_JWT_TOKEN = "X-DEVOPS-JWT-TOKEN"
const val AUTH_HEADER_IAM_TOKEN = "Authorization"

const val AUTH_HEADER_DEVOPS_SIGN_INFO = "X-DEVOPS-SIGN-INFO"

const val AUTH_HEADER_PLATFORM = "X-DEVOPS-PLATFORM"
const val AUTH_HEADER_APP_VERSION = "X-DEVOPS-APP-VERSION"

const val AUTH_HEADER_GATEWAY_TAG = "X-GATEWAY-TAG"

const val AUTH_HEADER_DEVOPS_CI_TASK_ID = "X-DEVOPS-CI-TASK-ID"

const val SIGN_HEADER_TIMESTAMP = "X-DEVOPS-TIMESTAMP"
const val SIGN_HEADER_NONCE = "X-DEVOPS-NONCE"
const val SING_HEADER_SIGNATURE = "X-DEVOPS-SIGNATURE"

const val AUTH_HEADER_GITHUB_TOKEN = "X-GITHUB-TOKEN"

const val AUTH_HEADER_GIT_TYPE = "X-DEVOPS-STREAM-GIT-TYPE"
const val AUTH_HEADER_BK_CI_LOGIN_TOKEN = "X-DEVOPS-CI-LOGIN-TOKEN"
const val AUTH_HEADER_DEVOPS_SERVICE_NAME = "X-DEVOPS-SERVICE-NAME"

const val AUTH_HEADER_CODECC_OPENAPI_TOKEN = "X-CODECC-OPENAPI-TOKEN"
