/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

/**
 *
 * @see com.tencent.devops.common.event.enums.PipelineBuildStatusBroadCastEventType
 */
enum class CallBackEvent {
    DELETE_PIPELINE,    /*流水线删除*/
    CREATE_PIPELINE,    /*流水线创建*/
    UPDATE_PIPELINE,    /*流水线更新，包括model和setting。*/
    STREAM_ENABLED,     /*stream ci 开启/关闭*/
    RESTORE_PIPELINE,   /*流水线恢复*/

    BUILD_START,        /*构建开始，不包含并发超限时排队、并发组排队。*/
    BUILD_END,          /*构建结束*/
    BUILD_STAGE_START,  /*stage开始*/
    BUILD_STAGE_END,    /*stage结束*/
    BUILD_JOB_START,    /*job开始，不包含BUILD_JOB_QUEUE。如果job SKIP或没有可执行的插件，就不会有该事件。*/
    BUILD_JOB_END,      /*job结束，job SKIP或没有可执行的插件时会有该事件。*/
    BUILD_TASK_START,   /*插件开始*/
    BUILD_TASK_END,     /*插件结束*/
    BUILD_TASK_PAUSE,   /*插件前置暂停、人工审核插件审核*/

    PROJECT_CREATE,     /*项目创建*/
    PROJECT_UPDATE,     /*项目更新*/
    PROJECT_ENABLE,     /*项目启用*/
    PROJECT_DISABLE     /*项目禁用*/
}

enum class MetricsEvent {
    BUILD_QUEUE,        /*构建排队，包含并发超限时排队、并发组排队。*/
    BUILD_START,        /*构建开始，不包含并发超限时排队、并发组排队。*/
    BUILD_END,          /*构建结束*/
    BUILD_QUALITY,      /*构建质量红线*/
    BUILD_STAGE_START,  /*stage开始*/
    BUILD_STAGE_PAUSE,  /*stage暂停、stage审核*/
    BUILD_STAGE_END,    /*stage结束*/
    BUILD_JOB_QUEUE,    /*job排队，包含互斥组排队、构建机复用互斥排队、最大job并发排队。*/
    BUILD_JOB_START,    /*job开始，不包含BUILD_JOB_QUEUE。如果job SKIP或没有可执行的插件，就不会有该事件。*/
    BUILD_JOB_END,      /*job结束，job SKIP或没有可执行的插件时会有该事件。*/
    BUILD_AGENT_START,  /*构建机启动，包含第三方构建机*/
    BUILD_AGENT_END,    /*构建机结束，包含第三方构建机*/
    BUILD_TASK_START,   /*插件开始*/
    BUILD_TASK_END,     /*插件结束*/
    BUILD_TASK_PAUSE,   /*插件前置暂停、人工审核插件审核*/
}

data class PipelineEvent(
    val pipelineId: String,
    val pipelineName: String,
    val userId: String,
    val updateTime: Long,
    val projectId: String
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
    val taskId: String?, // 仅当 BUILD_TASK_START/BUILD_TASK_END
    val buildNo: Int = 0, // 构建序号
    val debug: Boolean? // 是否为调试构建
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

data class ProjectCallbackEvent(
    val projectId: String,
    val projectName: String,
    val enable: Boolean,
    val userId: String
)

object CallbackConstants {
    // 项目级回调标志位
    const val DEVOPS_ALL_PROJECT = "DEVOPS_ALL_PROJECT"
}
