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

package com.tencent.devops.environment.pojo.job.jobresp

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "批量查询执行对象日志的结果")
data class QueryExecuteObjectLogsResult(
    @get:Schema(title = "作业实例ID", required = true)
    val jobInstanceId: Long,
    @get:Schema(title = "步骤实例ID", required = true)
    val stepInstanceId: Long,
    @get:Schema(title = "日志类型", description = "1-脚本执行任务日志，2-文件分发任务日志", required = true)
    val logType: Int,
    @get:Schema(title = "脚本执行任务日志列表，脚本任务才有该字段")
    val scriptStepExecuteObjectLogs: List<ScriptStepExecuteObjectLog>? = null,
    @get:Schema(title = "文件分发任务日志列表，文件分发任务才有该字段")
    val fileStepExecuteObjectLogs: List<FileStepExecuteObjectLog>? = null,
    @get:Schema(title = "作业平台请求ID，用于问题排查")
    val jobRequestId: String? = null
)

@Schema(title = "执行对象（主机）")
data class ExecuteObjectInfo(
    @get:Schema(title = "执行对象类型", description = "1-主机")
    val type: Int?,
    @get:Schema(title = "执行对象资源ID", description = "主机对应的资源ID为bk_host_id")
    val resourceId: String?,
    @get:Schema(title = "主机ID")
    val bkHostId: Long?,
    @get:Schema(title = "云区域ID")
    val bkCloudId: Long?,
    @get:Schema(title = "IP地址")
    val ip: String?
)

@Schema(title = "脚本执行任务日志")
data class ScriptStepExecuteObjectLog(
    @get:Schema(title = "执行对象")
    val executeObject: ExecuteObjectInfo?,
    @get:Schema(title = "日志内容")
    val logContent: String?
)

@Schema(title = "文件分发任务日志")
data class FileStepExecuteObjectLog(
    @get:Schema(title = "执行对象")
    val executeObject: ExecuteObjectInfo?,
    @get:Schema(title = "执行对象作为分发源时的上传日志")
    val uploadLogs: List<ExecuteObjectFileTaskLog>? = null,
    @get:Schema(title = "执行对象作为分发目标时的下载日志")
    val downloadLogs: List<ExecuteObjectFileTaskLog>? = null
)

@Schema(title = "单个文件的分发日志（一个源+一个目标+一个文件）")
data class ExecuteObjectFileTaskLog(
    @get:Schema(title = "文件分发模式", description = "0-上传，1-下载")
    val mode: Int?,
    @get:Schema(title = "源执行对象")
    val srcExecuteObject: ExecuteObjectInfo?,
    @get:Schema(title = "文件源路径")
    val srcPath: String?,
    @get:Schema(title = "目标执行对象，仅在mode=1时存在")
    val destExecuteObject: ExecuteObjectInfo? = null,
    @get:Schema(title = "文件目标路径，仅在mode=1时存在")
    val destPath: String? = null,
    @get:Schema(
        title = "文件传输任务状态",
        description = "0-从文件源拉取中，1-等待开始，2-上传中，3-下载中，4-完成，5-失败"
    )
    val status: Int?,
    @get:Schema(title = "文件传输日志内容")
    val logContent: String?,
    @get:Schema(title = "文件大小")
    val size: String?,
    @get:Schema(title = "上传/下载速度")
    val speed: String?,
    @get:Schema(title = "上传/下载进度")
    val process: String?
)
