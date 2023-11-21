package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class JobCloudFileStepInfo (
    @ApiModelProperty(value = "源文件列表")
    @JsonProperty("file_source_list")
    val jobCloudFileSourceList:List<JobCloudFileSource>
)