package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("文件分发任务日志")
@JsonIgnoreProperties(ignoreUnknown = true)
data class JobCloudFileDistributeLog(
    @ApiModelProperty(value = "云区域ID")
    @JsonProperty("bk_cloud_id")
    val bkCloudId: Long?,
    @ApiModelProperty(value = "IP地址")
    val ip: String?,
    @ApiModelProperty(value = "主机ID")
    @JsonProperty("bk_host_id")
    val bkHostId: Long?,
    @ApiModelProperty(value = "文件分发日志内容", required = true)
    @JsonProperty("file_logs")
    val jobCloudFileLogList: List<JobCloudFileLog>
)