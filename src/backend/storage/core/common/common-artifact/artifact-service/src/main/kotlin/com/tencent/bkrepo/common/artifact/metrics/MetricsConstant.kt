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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.common.artifact.metrics

const val ARTIFACT_UPLOADING_COUNT = "artifact.uploading.count"
const val ARTIFACT_UPLOADING_COUNT_DESC = "构件实时上传量"
const val ARTIFACT_DOWNLOADING_COUNT = "artifact.downloading.count"
const val ARTIFACT_DOWNLOADING_COUNT_DESC = "构件实时下载量"

const val ARTIFACT_UPLOADED_COUNT = "artifact.uploaded.count"
const val ARTIFACT_UPLOADED_COUNT_DESC = "构件上传总量"

const val ARTIFACT_DOWNLOADED_COUNT = "artifact.downloaded.count"
const val ARTIFACT_DOWNLOADED_COUNT_DESC = "构件下载总量"

const val ARTIFACT_UPLOADED_BYTES_COUNT = "artifact.uploaded.bytes.count"
const val ARTIFACT_UPLOADED_BYTES_COUNT_DESC = "构件上传字节数"

const val ARTIFACT_UPLOADED_CONSUME_COUNT = "artifact.uploaded.consume.count"
const val ARTIFACT_UPLOADED_CONSUME_COUNT_DESC = "构件上传耗时"

const val ARTIFACT_DOWNLOADED_BYTES_COUNT = "artifact.downloaded.bytes.count"
const val ARTIFACT_DOWNLOADED_BYTES_COUNT_DESC = "构件下载字节数"

const val ARTIFACT_DOWNLOADED_CONSUME_COUNT = "artifact.downloaded.consume.count"
const val ARTIFACT_DOWNLOADED_CONSUME_COUNT_DESC = "构件下载耗时"

const val ASYNC_TASK_ACTIVE_COUNT = "async.task.active.count"
const val ASYNC_TASK_ACTIVE_COUNT_DESC = "异步任务实时数量"

const val ASYNC_TASK_QUEUE_SIZE = "async.task.queue.size"
const val ASYNC_TASK_QUEUE_SIZE_DESC = "异步任务队列大小"
