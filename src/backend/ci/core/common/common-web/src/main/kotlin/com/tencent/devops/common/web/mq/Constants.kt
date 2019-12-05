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

package com.tencent.devops.common.web.mq

const val QUEUE_PIPELINE_BUILD = "queue_pipeline_build"
const val QUEUE_PIPELINE_BUILD_NEED_END = "queue_pipeline_build_need_end"
const val EXCHANGE_PIPELINE_BUILD = "exchange_pipeline_build"
const val ROUTE_PIPELINE_BUILD = "route_pipeline_build"
const val ROUTE_PIPELINE_BUILD_NEED_END = "route_pipeline_build_need_end"

const val ROUTE_PIPELINE_FINISH = "route_pipeline_finish"
const val QUEUE_PIPELINE_FINISH = "queue_pipeline_finish"
const val EXCHANGE_PIPELINE_FINISH = "exchange_pipeline_finish"

const val ROUTE_NOTIFY_MESSAGE = "route_notify_message"
const val QUEUE_NOTIFY_MESSAGE = "queue_notify_message"
const val EXCHANGE_NOTIFY_MESSAGE = "exchange_notify_message"

const val ROUTE_PAASCC_PROJECT_CREATE = "route_paascc_project_create"
const val QUEUE_PAASCC_PROJECT_CREATE = "queue_paascc_project_create"
const val EXCHANGE_PAASCC_PROJECT_CREATE = "exchange_paascc_project_create"

const val ROUTE_PAASCC_PROJECT_UPDATE = "route_paascc_project_update"
const val QUEUE_PAASCC_PROJECT_UPDATE = "queue_paascc_project_update"
const val EXCHANGE_PAASCC_PROJECT_UPDATE = "exchange_paascc_project_update"

const val EXCHANGE_PAASCC_PROJECT_UPDATE_LOGO = "exchange_paascc_project_update_logo"
const val ROUTE_PAASCC_PROJECT_UPDATE_LOGO = "route_paascc_project_update_logo"
const val QUEUE_PAASCC_PROJECT_UPDATE_LOGO = "queue_paascc_project_update_logo"

const val ROUTE_GIT_COMMIT_CHECK = "route_git_commit_check"
const val EXCHANGE_GIT_COMMIT_CHECK = "exchange_git_commit_check"
const val QUEUE_GIT_COMMIT_CHECK = "queue_git_commit_check"

const val ROUTE_GITHUB_PR = "route_github_pr"
const val EXCHANGE_GITHUB_PR = "exchange_github_pr"
const val QUEUE_GITHUB_PR = "queue_github_pr"