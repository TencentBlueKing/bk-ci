package com.tencent.devops.plugin.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("任务数据-上传文件任务数据")
data class FileTaskData(
    @ApiModelProperty("执行时间", required = true)
    override val cost: Int,
    @ApiModelProperty("文件名", required = true)
    val fileName: String,
    @ApiModelProperty("URL", required = true)
    val url: String
) : TaskData {
    companion object {
        const val classType = "fileTaskData"
    }
}