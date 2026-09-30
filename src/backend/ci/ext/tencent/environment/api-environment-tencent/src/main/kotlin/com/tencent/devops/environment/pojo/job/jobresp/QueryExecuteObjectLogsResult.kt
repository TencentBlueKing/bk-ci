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
    @get:Schema(title = "本次查询日志所用的步骤实例ID，固定为作业实例中的第一个步骤", required = true)
    val stepInstanceId: Long,
    @get:Schema(
        title = "日志类型：1-脚本执行任务日志（此时返回scriptStepExecuteObjectLogs），" +
            "2-文件分发任务日志（此时返回fileStepExecuteObjectLogs）",
        required = true
    )
    val logType: Int,
    @get:Schema(title = "脚本执行任务日志列表，每个元素对应一台主机，仅logType=1时存在")
    val scriptStepExecuteObjectLogs: List<ScriptStepExecuteObjectLog>? = null,
    @get:Schema(title = "文件分发任务日志列表，每个元素对应一台主机，仅logType=2时存在")
    val fileStepExecuteObjectLogs: List<FileStepExecuteObjectLog>? = null,
    @get:Schema(title = "作业平台请求ID，排查问题时提供给作业平台定位请求")
    val jobRequestId: String? = null
)

@Schema(title = "执行对象（主机）")
data class ExecuteObjectInfo(
    @get:Schema(title = "执行对象类型：1-主机，本接口仅支持主机")
    val type: Int?,
    @get:Schema(title = "执行对象资源ID，主机类型时取值为主机ID（bk_host_id）")
    val resourceId: String?,
    @get:Schema(title = "主机ID（CMDB中的bk_host_id）")
    val bkHostId: Long?,
    @get:Schema(title = "主机所在的管控区域ID（bk_cloud_id）")
    val bkCloudId: Long?,
    @get:Schema(title = "主机IP地址")
    val ip: String?
)

@Schema(title = "单台主机的脚本执行日志")
data class ScriptStepExecuteObjectLog(
    @get:Schema(title = "日志所属的执行对象（主机）")
    val executeObject: ExecuteObjectInfo?,
    @get:Schema(title = "脚本在该主机上执行输出的日志内容")
    val logContent: String?
)

@Schema(title = "单台主机的文件分发日志")
data class FileStepExecuteObjectLog(
    @get:Schema(title = "日志所属的执行对象（主机）")
    val executeObject: ExecuteObjectInfo?,
    @get:Schema(title = "该主机作为文件分发源时的上传日志列表，每个元素对应一个文件")
    val uploadLogs: List<ExecuteObjectFileTaskLog>? = null,
    @get:Schema(title = "该主机作为文件分发目标时的下载日志列表，每个元素对应一个源主机上的一个文件")
    val downloadLogs: List<ExecuteObjectFileTaskLog>? = null
)

@Schema(title = "单个文件的分发日志（一个源主机+一个目标主机+一个文件）")
data class ExecuteObjectFileTaskLog(
    @get:Schema(title = "文件分发模式：0-上传（源主机上传文件），1-下载（目标主机下载文件）")
    val mode: Int?,
    @get:Schema(title = "文件所在的源执行对象（主机）")
    val srcExecuteObject: ExecuteObjectInfo?,
    @get:Schema(title = "文件在源主机上的路径")
    val srcPath: String?,
    @get:Schema(title = "接收文件的目标执行对象（主机），仅mode=1时存在")
    val destExecuteObject: ExecuteObjectInfo? = null,
    @get:Schema(title = "文件在目标主机上的路径，仅mode=1时存在")
    val destPath: String? = null,
    @get:Schema(title = "文件传输任务状态：0-从文件源拉取中，1-等待开始，2-上传中，3-下载中，4-完成，5-失败")
    val status: Int?,
    @get:Schema(title = "文件传输的详细日志内容，包含文件名、大小、速度、进度、状态描述等信息")
    val logContent: String?,
    @get:Schema(title = "文件大小，带单位的字符串，例如：1.0 Bytes")
    val size: String?,
    @get:Schema(title = "上传/下载速度，带单位的字符串，例如：0 KB/s")
    val speed: String?,
    @get:Schema(title = "上传/下载进度，百分比字符串，例如：100%")
    val process: String?
)
