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

package com.tencent.devops.process.utils

const val PIPELINE_VERSION = "BK_CI_PIPELINE_VERSION" // "pipeline.version"
const val PIPELINE_START_PARENT_PROJECT_ID = "BK_CI_PARENT_PROJECT_ID"
const val PIPELINE_START_PARENT_PIPELINE_ID = "BK_CI_PARENT_PIPELINE_ID" // "pipeline.start.parent.pipeline.id"
const val PIPELINE_START_PARENT_BUILD_ID = "BK_CI_PARENT_BUILD_ID" // "pipeline.start.parent.build.id"
const val PIPELINE_START_PARENT_BUILD_TASK_ID = "BK_CI_PARENT_BUILD_TASK_ID" // "pipeline.start.parent.build.task.id"
const val PIPELINE_START_USER_ID = "BK_CI_START_USER_ID" // "pipeline.start.user.id"
const val PIPELINE_START_USER_NAME = "BK_CI_START_USER_NAME" // "pipeline.start.user.name"
const val PIPELINE_START_WEBHOOK_USER_ID = "BK_CI_START_WEBHOOK_USER_ID" // "pipeline.start.webhook.user.id"
const val PIPELINE_START_PIPELINE_USER_ID = "BK_CI_START_PIPELINE_USER_ID" // "pipeline.start.pipeline.user.id"
const val PIPELINE_START_SERVICE_USER_ID = "BK_CI_START_SERVICE_USER_ID" // "pipeline.start.service.user.id"
const val PIPELINE_START_MANUAL_USER_ID = "BK_CI_START_MANUAL_USER_ID" // "pipeline.start.manual.user.id"
const val PIPELINE_START_TIME_TRIGGER_USER_ID = "BK_CI_START_TIME_TRIGGER_USER_ID"
const val PIPELINE_START_REMOTE_USER_ID = "BK_CI_START_REMOTE_USER_ID"
const val PIPELINE_START_TYPE = "BK_CI_START_TYPE" // "pipeline.start.type"
const val PIPELINE_START_CHANNEL = "BK_CI_START_CHANNEL" // "pipeline.start.channel"
const val PIPELINE_BUILD_NUM = "BK_CI_BUILD_NUM" // "pipeline.build.num"// 当前构建的唯一标示ID，从1开始自增
const val PIPELINE_BUILD_LAST_UPDATE = "BK_CI_BUILD_LAST_UPDATE" // "pipeline.build.last.update"
const val PIPELINE_BUILD_SVN_REVISION = "BK_CI_BUILD_SVN_REVISION" // "pipeline.build.svn.revision"
const val PIPELINE_BUILD_NUM_ALIAS = "BK_CI_BUILD_NUM_ALIAS"
const val PIPELINE_BUILD_URL = "BK_CI_BUILD_URL"

const val GIT_MR_NUMBER = "BK_CI_GIT_MR_NUMBER" // git_mr_number
const val GITHUB_PR_NUMBER = "BK_CI_GITHUB_PR_NUMBER" // github_pr_number

const val PIPELINE_NAME = "BK_CI_PIPELINE_NAME" // "pipeline.name"
const val PIPELINE_ID = "BK_CI_PIPELINE_ID" // "pipeline.id"
const val WORKSPACE = "WORKSPACE" // "ci.workspace"

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
const val PIPELINE_RETRY_ALL_FAILED_CONTAINER = "BK_CI_RETRY_ALL_FAILED_CONTAINER"
const val PIPELINE_SKIP_FAILED_TASK = "BK_CI_SKIP_FAILED_TASK"

const val BK_CI_BUILD_FAIL_TASKS = "BK_CI_BUILD_FAIL_TASKS"
const val BK_CI_BUILD_FAIL_TASKNAMES = "BK_CI_BUILD_FAIL_TASKNAMES"

const val PIPELINE_VIEW_MY_PIPELINES = "myPipeline"
const val PIPELINE_VIEW_MY_LIST_PIPELINES = "myListPipeline" // 兼容APP , 后面需要干掉
const val PIPELINE_VIEW_FAVORITE_PIPELINES = "collect"
const val PIPELINE_VIEW_ALL_PIPELINES = "allPipeline"
const val PIPELINE_VIEW_UNCLASSIFIED = "unclassified" // 流水线视图未分组
const val PIPELINE_VIEW_RECENT_USE = "recentUse"

const val PIPELINE_MATERIAL_URL = "BK_CI_PIEPLEINE_MATERIAL_URL" // pipeline.material.url
const val PIPELINE_MATERIAL_BRANCHNAME = "BK_CI_PIPELINE_MATERIAL_BRANCHNAME" // pipeline.material.branchName
const val PIPELINE_MATERIAL_ALIASNAME = "BK_CI_PIPELINE_MATERIAL_ALIASNAME" // pipeline.material.aliasName
const val PIPELINE_MATERIAL_NEW_COMMIT_ID = "BK_CI_PIPELINE_MATERIAL_NEW_COMMIT_ID" // pipeline.material.new.commit.id
const val PIPELINE_MATERIAL_NEW_COMMIT_COMMENT =
    "BK_CI_PIPELINE_MATERIAL_NEW_COMMIT_COMMENT" // pipeline.material.new.commit.comment
const val PIPELINE_MATERIAL_NEW_COMMIT_TIMES =
    "BK_CI_PIPELINE_MATERIAL_NEW_COMMIT_TIMES" // pipeline.material.new.commit.times

const val MAJORVERSION = "BK_CI_MAJOR_VERSION" // MajorVersion
const val MINORVERSION = "BK_CI_MINOR_VERSION" // MinorVersion
const val FIXVERSION = "BK_CI_FIX_VERSION" // FixVersion
const val BUILD_NO = "BK_CI_BUILD_NO" // "BuildNo"
const val BUILD_STATUS = "BK_CI_BUILD_STATUS" // "BuildStatus"

/**
 * 后续新增的变量统一用“BK_CI_大写的变量名称”命名，历史的变量名称统一整改
 */
const val PIPELINE_CREATE_USER = "BK_CI_PIPELINE_CREATE_USER" // "流水线创建用户"
const val PIPELINE_UPDATE_USER = "BK_CI_PIPELINE_UPDATE_USER" // "流水线最后更新用户"
const val PIPELINE_BUILD_REMARK = "BK_CI_BUILD_REMARK" // "流水线构建备注"
const val PIPELINE_ATOM_NAME = "BK_CI_ATOM_NAME" // "流水线插件名称"
const val PIPELINE_ATOM_CODE = "BK_CI_ATOM_CODE" // "流水线插件代码"
const val PIPELINE_ATOM_VERSION = "BK_CI_ATOM_VERSION" // "流水线插件版本"
const val PIPELINE_TASK_NAME = "BK_CI_TASK_NAME" // "流水线任务名称（步骤名称）"
const val PIPELINE_STEP_ID = "BK_CI_STEP_ID" // "用户自定义ID（上下文标识）"
const val PIPELINE_ATOM_TIMEOUT = "BK_CI_ATOM_TIMEOUT" // "流水线插件超时时间"

/**
 * 流水线设置-最大排队数量-默认值
 */
const val PIPELINE_SETTING_MAX_QUEUE_SIZE_DEFAULT = 10

/**
 * 流水线插件设置-失败重试最大值
 */
const val TASK_FAIL_RETRY_MAX_COUNT = 5

/**
 * 流水线插件设置-失败重试最小值
 */
const val TASK_FAIL_RETRY_MIN_COUNT = 1

/**
 * 流水线设置-最大排队数量-最小值
 */
const val PIPELINE_SETTING_MAX_QUEUE_SIZE_MIN = 0

/**
 * 流水线设置-最大排队数量-最大值
 */
const val PIPELINE_SETTING_MAX_QUEUE_SIZE_MAX = 200

/**
 * 流水线设置-最大并发数量-默认值
 */
const val PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_DEFAULT = 50

/**
 * 流水线设置-最大并发数量-最大值
 */
const val PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX = 200

/**
 * 流水线设置-最大排队时间-默认值 单位:分钟
 */
const val PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_DEFAULT = 1

/**
 * 流水线设置-最大排队时间-最小值 单位:分钟
 */
const val PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MIN = 1

/**
 * 流水线设置-最大排队时间-默认值 单位:分钟
 */
const val PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MAX = 1440

/**
 * 流水线设置-插件错误信息入库长度最大值 单位:分钟
 */
const val PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX = 4000

/**
 * 流水线设置-流水线错误信息入库长度最大值 单位:分钟
 */
const val PIPELINE_MESSAGE_STRING_LENGTH_MAX = 30000

/**
 * 流水线设置-流水线最大任务并发数量-最大值
 */
const val PIPELINE_CON_RUNNING_CONTAINER_SIZE_MAX = 30

/**
 * 流水线设置-矩阵内最大并发数量-默认值
 */
const val PIPELINE_MATRIX_MAX_CON_RUNNING_SIZE_DEFAULT = 5

/**
 * 流水线设置-矩阵内最大并发数量-最大值
 */
const val PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX = 20

/**
 * 流水线设置-Stage内最大分裂后Job数量-最大值
 */
const val PIPELINE_STAGE_CONTAINERS_COUNT_MAX = 256

/**
 * 入库VAR表，流水线变量最大长度
 */
const val PIPELINE_VARIABLES_STRING_LENGTH_MAX = 4000

const val PIPELINE_TIME_START = "BK_CI_BUILD_START_TIME" // "pipeline.time.start"

const val PIPELINE_TIME_END = "BK_CI_BUILD_END_TIME" // "pipeline.time.end"

const val PIPELINE_BUILD_MSG = "BK_CI_BUILD_MSG"

/**
 * 流水线设置-CONCURRENCY GROUP 并发组-默认值
 */
const val PIPELINE_SETTING_CONCURRENCY_GROUP_DEFAULT = "\${{ci.pipeline_id}}"

/**
 * 保存流水线编排的最大个数
 */
const val PIPELINE_RES_NUM_MIN = 50

const val KEY_PIPELINE_ID = "pipelineId"

const val KEY_PIPELINE_NAME = "pipelineName"

const val KEY_PROJECT_ID = "projectId"

const val KEY_TEMPLATE_ID = "templateId"

const val KEY_STAGE = "stage"

const val KEY_JOB = "job"

const val KEY_TASK = "task"
