package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "批量查询执行对象日志的结果")
@JsonIgnoreProperties(ignoreUnknown = true)
data class JobCloudQueryExecuteObjectLogsResult(
    @get:Schema(title = "作业实例ID", required = true)
    @JsonProperty("job_instance_id")
    val jobInstanceId: Long = -1,
    @get:Schema(title = "步骤实例ID", required = true)
    @JsonProperty("step_instance_id")
    val stepInstanceId: Long = -1,
    @get:Schema(title = "日志类型", description = "1-脚本执行任务日志，2-文件分发任务日志", required = true)
    @JsonProperty("log_type")
    val logType: Int = -1,
    @get:Schema(title = "脚本执行任务日志列表")
    @JsonProperty("script_step_execute_object_logs")
    val scriptStepExecuteObjectLogs: List<JobCloudScriptStepExecuteObjectLog>? = null,
    @get:Schema(title = "文件分发任务日志列表")
    @JsonProperty("file_step_execute_object_logs")
    val fileStepExecuteObjectLogs: List<JobCloudFileStepExecuteObjectLog>? = null
)

@Schema(title = "执行对象")
@JsonIgnoreProperties(ignoreUnknown = true)
data class JobCloudExecuteObject(
    @get:Schema(title = "执行对象类型", description = "1-主机，2-容器")
    @JsonProperty("type")
    val type: Int? = null,
    @get:Schema(title = "执行对象资源ID")
    @JsonProperty("resource_id")
    val resourceId: String? = null,
    @get:Schema(title = "主机详情，执行对象是主机时存在")
    @JsonProperty("host")
    val host: JobCloudExecuteObjectHost? = null
)

@Schema(title = "执行对象主机详情")
@JsonIgnoreProperties(ignoreUnknown = true)
data class JobCloudExecuteObjectHost(
    @get:Schema(title = "主机ID")
    @JsonProperty("bk_host_id")
    val bkHostId: Long? = null,
    @get:Schema(title = "云区域ID")
    @JsonProperty("bk_cloud_id")
    val bkCloudId: Long? = null,
    @get:Schema(title = "IP地址")
    @JsonProperty("ip")
    val ip: String? = null
)

@Schema(title = "脚本执行任务日志")
@JsonIgnoreProperties(ignoreUnknown = true)
data class JobCloudScriptStepExecuteObjectLog(
    @get:Schema(title = "执行对象")
    @JsonProperty("execute_object")
    val executeObject: JobCloudExecuteObject? = null,
    @get:Schema(title = "日志内容")
    @JsonProperty("log_content")
    val logContent: String? = null
)

@Schema(title = "文件分发任务日志")
@JsonIgnoreProperties(ignoreUnknown = true)
data class JobCloudFileStepExecuteObjectLog(
    @get:Schema(title = "执行对象")
    @JsonProperty("execute_object")
    val executeObject: JobCloudExecuteObject? = null,
    @get:Schema(title = "上传日志")
    @JsonProperty("upload_log")
    val uploadLog: List<JobCloudFileTaskLog>? = null,
    @get:Schema(title = "下载日志")
    @JsonProperty("download_log")
    val downloadLog: List<JobCloudFileTaskLog>? = null
)

@Schema(title = "单个文件的分发日志")
@JsonIgnoreProperties(ignoreUnknown = true)
data class JobCloudFileTaskLog(
    @get:Schema(title = "文件分发模式", description = "0-上传，1-下载")
    @JsonProperty("mode")
    val mode: Int? = null,
    @get:Schema(title = "源执行对象")
    @JsonProperty("src_execute_object")
    val srcExecuteObject: JobCloudExecuteObject? = null,
    @get:Schema(title = "文件源路径")
    @JsonProperty("src_path")
    val srcPath: String? = null,
    @get:Schema(title = "目标执行对象")
    @JsonProperty("dest_execute_object")
    val destExecuteObject: JobCloudExecuteObject? = null,
    @get:Schema(title = "文件目标路径")
    @JsonProperty("dest_path")
    val destPath: String? = null,
    @get:Schema(title = "文件传输任务状态")
    @JsonProperty("status")
    val status: Int? = null,
    @get:Schema(title = "文件传输日志内容")
    @JsonProperty("log_content")
    val logContent: String? = null,
    @get:Schema(title = "文件大小")
    @JsonProperty("size")
    val size: String? = null,
    @get:Schema(title = "上传/下载速度")
    @JsonProperty("speed")
    val speed: String? = null,
    @get:Schema(title = "上传/下载进度")
    @JsonProperty("process")
    val process: String? = null
)
