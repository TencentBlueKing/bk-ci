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

package com.tencent.devops.sign.api.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("SignHistory-IPA包签名信息")
data class SignHistory(
    @ApiModelProperty("签名ID", required = true)
    val resignId: String,
    @ApiModelProperty("操作用户", required = true)
    val userId: String,
    @ApiModelProperty("文件MD5", required = false)
    val md5: String?,
    @ApiModelProperty("结果文件名称", required = false)
    val resultFileName: String? = "",
    @ApiModelProperty("结果文件MD5", required = false)
    val resultFileMd5: String? = "",
    @ApiModelProperty("归档类型(PIPELINE|CUSTOM)", required = false)
    val archiveType: String?,
    @ApiModelProperty("项目Id", required = false)
    val projectId: String?,
    @ApiModelProperty("流水线Id", required = false)
    val pipelineId: String?,
    @ApiModelProperty("构建ID", required = false)
    val buildId: String?,
    @ApiModelProperty("插件ID", required = false)
    val taskId: String? = null,
    @ApiModelProperty("归档路径", required = false)
    val archivePath: String?,
    @ApiModelProperty("任务状态", required = false)
    val status: String?,
    @ApiModelProperty("创建时间", required = true)
    val createTime: Long?,
    @ApiModelProperty("完成时间", required = false)
    val endTime: Long?,
    @ApiModelProperty("上传完成时间", required = false)
    val uploadFinishTime: Long?,
    @ApiModelProperty("解压完成时间", required = false)
    val unzipFinishTime: Long?,
    @ApiModelProperty("签名完成时间", required = false)
    val resignFinishTime: Long?,
    @ApiModelProperty("压缩完成时间", required = false)
    val zipFinishTime: Long?,
    @ApiModelProperty("归档完成时间", required = false)
    val archiveFinishTime: Long?,
    @ApiModelProperty("错误信息", required = false)
    val errorMessage: String?,
    @ApiModelProperty("签名任务请求原文", required = false)
    var ipaSignInfoStr: String?
)
