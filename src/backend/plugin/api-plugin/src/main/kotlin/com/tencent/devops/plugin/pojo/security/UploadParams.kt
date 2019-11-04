/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.plugin.pojo.security

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("任务数据-上传文件任务数据")
data class UploadParams(
    @ApiModelProperty(value = "文件路径", required = true)
    val filePath: String,
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String,
    @ApiModelProperty("构建ID", required = true)
    val buildId: String,
    @ApiModelProperty("构建No", required = true)
    val buildNo: Int,
    @ApiModelProperty("原子ID", required = true)
    val elementId: String,
    @ApiModelProperty("容器ID", required = true)
    val containerId: String,
    @ApiModelProperty("执行次数", required = true)
    val executeCount: Int,
    @ApiModelProperty(value = "是否是自定义仓库", required = true)
    val custom: Boolean,
    @ApiModelProperty(value = "执行用户", required = true)
    val userId: String,
    @ApiModelProperty(value = "环境id", required = true)
    val envId: String,
    @ApiModelProperty(value = "文件大小", required = true)
    val fileSize: String,
    @ApiModelProperty(value = "文件md5", required = true)
    val fileMd5: String,
    @ApiModelProperty(value = "app版本号", required = true)
    val appVersion: String,
    @ApiModelProperty(value = "app名称", required = true)
    val appTitle: String,
    @ApiModelProperty(value = "app包名", required = true)
    val packageName: String
)