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

package com.tencent.devops.common.pipeline.event

/*
// 构建事件
{
    "event": "BUILD_START/BUILD_END/BUILD_TASK_START/BUILD_TASK_END/BUILD_STAGE_START/BUILD_STAGE_END",
    "data": {
        "pipelineId": "流水线ID",
        "pipelineName": "流水线名称",
        "userId": "操作人",
        "status": "状态 大写英文 比如RUNNING 见左边说明",
        "startTime": "开始时间毫秒数，如果未启动，则该字段为0",
        "endTime": "结束时间毫秒数，如果未结束，则该字段为0",
        "model": {
            "stages": [
                {
                    "stageName": "Stage名称",
                    "status": "状态 大写英文 比如RUNNING 见左边说明",
                    "startTime": "开始时间毫秒数，如果未启动，则该字段为0",
                    "endTime": "结束时间毫秒数，如果未结束，则该字段为0",
                    "jobs": [
                        {
                            "jobName": "job名称",
                            "status": "状态 大写英文 比如RUNNING 见说明",
                            "startTime": "开始时间毫秒数，如果未启动，则该字段为0",
                            "endTime": "结束时间毫秒数，如果未结束，则该字段为0",
                            "tasks": [
                                {
                                    "taskId": "插件任务ID",
                                    "taskName": "插件任务名称",
                                    "atomCode": "研发商店的插件ID",
                                    "status": "状态  =大写英文 比如RUNNING 见说明",
                                    "startTime": "开始时间毫秒数，如果未启动，则该字段为0",
                                    "endTime": "结束时间毫秒数，如果未结束，则该字段为0",
                                }
                            ]
                        }
                    ]
                }
            ]
        }
    }
}
// 删除/创建/编辑 流水线事件
{
    "event": "DELETE_PIPELINE/CREATE_PIPELINE/UPDATE_PIPELINE",
    "data": {
        "pipelineId": "流水线ID",
        "pipelineName": "流水线名称",
        "userId": "操作人",
        "updateTime": "操作时间，毫秒数"
    }
}
*/

class CallBackData<out T>(
    val event: CallBackEvent,
    val data: T
)

enum class CallBackEvent {
    DELETE_PIPELINE,
    CREATE_PIPELINE,
    UPDATE_PIPELINE,
    STREAM_ENABLED,
    RESTORE_PIPELINE,
    BUILD_START,
    BUILD_END,
    BUILD_TASK_START,
    BUILD_TASK_END,
    BUILD_STAGE_START,
    BUILD_STAGE_END,
    BUILD_TASK_PAUSE
}

data class PipelineEvent(
    val pipelineId: String,
    val pipelineName: String,
    val userId: String,
    val updateTime: Long
)

data class StreamEnabledEvent(
    val userId: String,
    val gitProjectId: Long,
    val gitProjectUrl: String,
    val enable: Boolean
)

data class BuildEvent(
    val buildId: String,
    val pipelineId: String,
    val pipelineName: String,
    val userId: String,
    val triggerUser: String? = null,
    val cancelUserId: String? = null,
    val status: String,
    val startTime: Long = 0,
    val endTime: Long = 0,
    val model: SimpleModel,
    val projectId: String,
    val trigger: String,
    val stageId: String?, // 仅当 BUILD_STAGE_START/BUILD_STAGE_END
    val taskId: String? // 仅当 BUILD_TASK_START/BUILD_TASK_END
)

data class SimpleModel(
    val stages: List<SimpleStage>
)

data class SimpleStage(
    val stageName: String,
    val name: String, // 有业务场景会根据真实的stage-name做逻辑。 如id: stage-1,用户改名为"阶段1",会根据"阶段1"做逻辑
    var status: String,
    var startTime: Long = 0,
    var endTime: Long = 0,
    val jobs: List<SimpleJob>
)

data class SimpleJob(
    val jobName: String,
    val status: String,
    val startTime: Long = 0,
    val endTime: Long = 0,
    val tasks: List<SimpleTask>
)

data class SimpleTask(
    val taskId: String,
    val taskName: String,
    val atomCode: String,
    val status: String,
    val startTime: Long = 0,
    val endTime: Long = 0
)
