package com.tencent.devops.process.pojo.third.spm

import io.swagger.annotations.ApiModelProperty

data class SpmFileInfo(
    @ApiModelProperty("file_id")
    val fileId: Int,
    @ApiModelProperty("batch_id")
    val batchId: Int,
    @ApiModelProperty("operate_type")
    val operateType: String,
    @ApiModelProperty("filename")
    val fileName: String,
    @ApiModelProperty("size")
    val size: Int,
    @ApiModelProperty("md5")
    val md5: String,
    @ApiModelProperty("status")
    val status: Int,
    @ApiModelProperty("submit_time")
    val submitTime: String,
    @ApiModelProperty("finish_rate")
    val finishRate: String
)