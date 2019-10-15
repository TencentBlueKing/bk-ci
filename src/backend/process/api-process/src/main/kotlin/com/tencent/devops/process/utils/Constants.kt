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

package com.tencent.devops.process.utils

const val PIPELINE_VERSION = "BK_CI_PIPELINE_VERSION" // "pipeline.version"
const val PIPELINE_START_PARENT_PIPELINE_ID = "BK_CI_PARENT_PIPELINE_ID" // "pipeline.start.parent.pipeline.id"
const val PIPELINE_START_PARENT_BUILD_ID = "BK_CI_PARENT_BUILD_ID" // "pipeline.start.parent.build.id"
const val PIPELINE_START_PARENT_BUILD_TASK_ID =
    "BK_CI_PARENT_BUILD_TASK_ID" // "pipeline.start.parent.build.task.id"
const val PIPELINE_START_USER_ID = "BK_CI_START_USER_ID" // "pipeline.start.user.id"
const val PIPELINE_START_USER_NAME = "BK_CI_START_USER_NAME" // "pipeline.start.user.name"
const val PIPELINE_START_WEBHOOK_USER_ID = "BK_CI_START_WEBHOOK_USER_ID" // "pipeline.start.webhook.user.id"
const val PIPELINE_START_PIPELINE_USER_ID = "BK_CI_START_PIPELINE_USER_ID" // "pipeline.start.pipeline.user.id"
const val PIPELINE_START_TYPE = "BK_CI_START_TYPE" // "pipeline.start.type"
const val PIPELINE_START_CHANNEL = "BK_CI_START_CHANNEL" // "pipeline.start.channel"
const val PIPELINE_BUILD_NUM = "BK_CI_BUILD_NUM" // "pipeline.build.num"
const val PIPELINE_BUILD_SVN_REVISION = "BK_CI_BUILD_SVN_REVISION" // "pipeline.build.svn.revision"

const val PIPELINE_WEBHOOK_REVISION = "BK_CI_HOOK_REVISION"
const val PIPELINE_WEBHOOK_BRANCH = "BK_CI_HOOK_BRANCH"
const val PIPELINE_WEBHOOK_SOURCE_BRANCH = "BK_CI_HOOK_SOURCE_BRANCH"
const val PIPELINE_WEBHOOK_TARGET_BRANCH = "BK_CI_HOOK_TARGET_BRANCH"
const val PIPELINE_WEBHOOK_SOURCE_URL = "BK_CI_HOOK_SOURCE_URL"
const val PIPELINE_WEBHOOK_TARGET_URL = "BK_CI_HOOK_TARGET_URL"
const val PIPELINE_WEBHOOK_REPO = "BK_CI_HOOK_REPO"
const val PIPELINE_WEBHOOK_REPO_TYPE = "BK_CI_HOOK_REPO_TYPE"
const val PIPELINE_WEBHOOK_BLOCK = "BK_CI_HOOK_BLOCK"
const val PIPELINE_WEBHOOK_TYPE = "BK_CI_HOOK_TYPE"
const val PIPELINE_WEBHOOK_EVENT_TYPE = "BK_CI_HOOK_EVENT_TYPE"
const val PIPELINE_WEBHOOK_MR_ID = "BK_CI_HOOK_MR_ID"

const val GIT_MR_NUMBER = "BK_CI_GIT_MR_NUMBER"
const val GITHUB_PR_NUMBER = "BK_CI_GITHUB_PR_NUMBER"

const val PIPELINE_NAME = "BK_CI_PIPELINE_NAME" // "pipeline.name"
const val PIPELINE_ID = "BK_CI_PIPELINE_ID" // "pipeline.id"

const val PIPELINE_TIME_DURATION = "BK_CI_BUILD_TOTAL_TIME" // "pipeline.time.duration"

const val PIPELINE_BUILD_ID = "BK_CI_BUILD_ID" // "pipeline.build.id"
const val PIPELINE_VMSEQ_ID = "BK_CI_BUILD_JOB_ID" // "pipeline.job.id"
const val PIPELINE_ELEMENT_ID = "BK_CI_BUILD_TASK_ID" // "pipeline.task.id"
const val PIPELINE_TURBO_TASK_ID = "BK_CI_TURBO_ID" // "turbo.task.id"
const val PROJECT_NAME = "BK_CI_PROJECT_NAME" // "project.name"
const val REPORT_DYNAMIC_ROOT_URL = "BK_CI_REPORT_DYNAMIC_ROOT_URL" // "report.dynamic.root.url"

const val PROJECT_NAME_CHINESE = "BK_CI_PROJECT_NAME_CN" // "project.name.chinese"

const val PIPELINE_START_MOBILE = "BK_CI_IS_MOBILE" // "pipeline.start.isMobile"

const val PIPELINE_START_TASK_ID = "BK_CI_START_TASK_ID" // "pipeline.start.task.id"
const val PIPELINE_RETRY_COUNT = "BK_CI_RETRY_COUNT" // "pipeline.retry.count"
const val PIPELINE_RETRY_BUILD_ID = "BK_CI_RETRY_BUILD_ID" // "pipeline.retry.build.id"
const val PIPELINE_RETRY_START_TASK_ID = "BK_CI_RETRY_TASK_ID" // "pipeline.retry.start.task.id"
const val PIPELINE_START_REPO_NAME = "BK_CI_REPOSITORY_NAME" // "pipeline.retry.start.task.id"

const val PIPELINE_VIEW_MY_PIPELINES = "myPipeline"
const val PIPELINE_VIEW_FAVORITE_PIPELINES = "collect"
const val PIPELINE_VIEW_ALL_PIPELINES = "allPipeline"

const val PIPELINE_MATERIAL_URL = "BK_CI_PIEPLEINE_MATERIAL_URL"
const val PIPELINE_MATERIAL_BRANCHNAME = "BK_CI_PIPELINE_MATERIAL_BRANCHNAME"
const val PIPELINE_MATERIAL_ALIASNAME = "BK_CI_PIPELINE_MATERIAL_ALIASNAME"
const val PIPELINE_MATERIAL_NEW_COMMIT_ID = "BK_CI_PIPELINE_MATERIAL_NEW_COMMIT_ID"
const val PIPELINE_MATERIAL_NEW_COMMIT_COMMENT = "BK_CI_PIPELINE_MATERIAL_NEW_COMMIT_COMMENT"
const val PIPELINE_MATERIAL_NEW_COMMIT_TIMES = "BK_CI_PIPELINE_MATERIAL_NEW_COMMIT_TIMES"

const val MAJORVERSION = "BK_CI_MAJOR_VERSION" // majorVersion
const val MINORVERSION = "BK_CI_MINOR_VERSION" // minorVersion
const val FIXVERSION = "BK_CI_FIX_VERSION" // fixVersion
const val BUILD_NO = "BK_CI_BUILD_NO" // "BuildNo"

/**
 * 流水线设置-最大排队数量-默认值
 */
const val PIPELINE_SETTING_MAX_QUEUE_SIZE_DEFAULT = 10
/**
 * 流水线设置-最大排队数量-最小值
 */
const val PIPELINE_SETTING_MAX_QUEUE_SIZE_MIN = 0
/**
 * 流水线设置-最大排队数量-最大值
 */
const val PIPELINE_SETTING_MAX_QUEUE_SIZE_MAX = 20

/**
 * 流水线设置-最大排队时间-默认值 单位:分钟
 */
const val PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_DEFAULT = 120
/**
 * 流水线设置-最大排队时间-最小值 单位:分钟
 */
const val PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MIN = 1
/**
 * 流水线设置-最大排队时间-默认值 单位:分钟
 */
const val PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MAX = 1440