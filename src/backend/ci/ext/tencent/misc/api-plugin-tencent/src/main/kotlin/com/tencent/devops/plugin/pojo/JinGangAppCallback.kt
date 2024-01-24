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

package com.tencent.devops.plugin.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("金刚app扫描回调结果")
data class JinGangAppCallback(
    @ApiModelProperty("0表示成功，其他表示失败")
    val status: String,
    @ApiModelProperty("失败提示信息，成功则为空")
    val msg: String,
    @ApiModelProperty("构建号")
    val buildId: String,
    @ApiModelProperty("构建下面对应的任务号")
    val taskId: String,
    @ApiModelProperty("element ID")
    val elementId: String,
    @ApiModelProperty("该次扫描文件md5")
    val md5: String,
    @ApiModelProperty("结果html地址", name = "scan_url")
    @JsonProperty("scan_url")
    val scanUrl: String,
    @ApiModelProperty("结果xml下载地址", name = "scan_xml")
    @JsonProperty("scan_xml")
    val scanXml: String,
    @ApiModelProperty("上传人", name = "responseuser")
    @JsonProperty("responseuser")
    val responseUser: String

)
