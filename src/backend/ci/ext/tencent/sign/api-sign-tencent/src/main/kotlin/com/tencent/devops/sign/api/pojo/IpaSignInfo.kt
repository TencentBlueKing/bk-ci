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

@ApiModel("IpaSignInfo-IPA包签名信息")
data class IpaSignInfo(
    @ApiModelProperty("操作用户", required = true)
    var userId: String = "",
    @ApiModelProperty("是否采用通配符重签", required = true)
    var wildcard: Boolean = true,
    @ApiModelProperty("文件名称", required = true)
    var fileName: String = "",
    @ApiModelProperty("文件大小", required = false)
    var fileSize: Long = 0L,
    @ApiModelProperty("文件MD5", required = false)
    var md5: String = "",
    @ApiModelProperty("证书ID", required = false)
    var certId: String = "",
    @ApiModelProperty("归档类型(PIPELINE|CUSTOM)", required = false)
    var archiveType: String = "PIPELINE",
    @ApiModelProperty("项目ID", required = false)
    var projectId: String = "",
    @ApiModelProperty("流水线ID", required = false)
    var pipelineId: String? = null,
    @ApiModelProperty("构建ID", required = false)
    var buildId: String? = null,
    @ApiModelProperty("构建号", required = false)
    var buildNum: Int? = null,
    @ApiModelProperty("任务ID", required = false)
    var taskId: String? = null,
    @ApiModelProperty("结果文件名后缀", required = false)
    var resultSuffix: String? = "_enterprise_sign",
    @ApiModelProperty("归档路径", required = false)
    var archivePath: String? = "/",
    @ApiModelProperty("主App描述文件ID", required = false)
    var mobileProvisionId: String? = null,
    @ApiModelProperty("Universal Link的设置", required = false)
    var universalLinks: List<String>? = null,
    @ApiModelProperty("安全应用组，应为securityApplicationGroupList", required = false)
    var keychainAccessGroups: List<String>? = null,
    @ApiModelProperty("是否替换bundleId", required = false)
    var replaceBundleId: Boolean? = false,
    @ApiModelProperty("拓展应用名和对应的描述文件ID", required = false)
    var appexSignInfo: List<AppexSignInfo>? = null,
    @ApiModelProperty("待替换的plist信息", required = false)
    var replaceKeyList: Map<String, String>? = null,
    @ApiModelProperty("指定xcode签名工具版本", required = false)
    var codeSignVersion: String? = null,
    @ApiModelProperty("codesign插件额外参数", required = false)
    var codesignExternalStr: String? = null,
    @ApiModelProperty("钥匙串访问组", required = false)
    var keychainAccessGroupList: List<String>? = null
)
