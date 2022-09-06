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

package com.tencent.bkrepo.job

/**
 * 分表数量
 */
const val SHARDING_COUNT = 256

/**
 * mongodb 最小id
 */
const val MIN_OBJECT_ID = "000000000000000000000000"

/**
 * 一次处理数据量
 */
const val BATCH_SIZE = 1000

/**
 * 数据库字段
 */
const val ID = "_id"
const val SHA256 = "sha256"
const val PROJECT = "projectId"
const val REPO = "repoName"
const val FOLDER = "folder"
const val CREDENTIALS = "credentialsKey"
const val COUNT = "count"
const val TYPE = "type"
const val CATEGORY = "category"
const val CREATED_DATE = "createdDate"

/**
 * metrics
 */
const val ASYNC_TASK_ACTIVE_COUNT = "async.task.active.count"
const val ASYNC_TASK_ACTIVE_COUNT_DESC = "异步任务实时数量"

const val ASYNC_TASK_QUEUE_SIZE = "async.task.queue.size"
const val ASYNC_TASK_QUEUE_SIZE_DESC = "异步任务队列大小"

const val RUNNING_TASK_JOB_COUNT = "running.job.count"
const val RUNNING_TASK_JOB_DESC = "运行中的任务数量"

const val JOB_TASK_COUNT = "job.task.count"
const val JOB_TASK_COUNT_DESC = "任务执行统计"
const val JOB_AVG_TIME_CONSUME = "job.avg.time.consume"
const val JOB_AVG_WAIT_TIME_CONSUME_DESC = "任务平均等待时长统计"
const val JOB_AVG_EXECUTE_TIME_CONSUME_DESC = "任务平均执行时长统计"
