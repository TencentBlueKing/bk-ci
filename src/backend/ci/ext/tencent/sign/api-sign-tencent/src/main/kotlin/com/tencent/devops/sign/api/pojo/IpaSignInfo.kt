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

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "IpaSignInfo-IPA包签名信息")
data class IpaSignInfo(
    @get:Schema(title = "操作用户", required = true)
    var userId: String = "",
    @get:Schema(title = "是否采用通配符重签", required = true)
    var wildcard: Boolean = true,
    @get:Schema(title = "文件名称", required = true)
    var fileName: String = "",
    @get:Schema(title = "文件大小", required = false)
    var fileSize: Long = 0L,
    @get:Schema(title = "文件MD5", required = false)
    var md5: String = "",
    @get:Schema(title = "证书ID", required = false)
    var certId: String = "",
    @get:Schema(title = "归档类型(PIPELINE|CUSTOM)", required = false)
    var archiveType: String = "PIPELINE",
    @get:Schema(title = "项目ID", required = false)
    var projectId: String = "",
    @get:Schema(title = "流水线ID", required = false)
    var pipelineId: String? = null,
    @get:Schema(title = "构建ID", required = false)
    var buildId: String? = null,
    @get:Schema(title = "构建号", required = false)
    var buildNum: Int? = null,
    @get:Schema(title = "任务ID", required = false)
    var taskId: String? = null,
    @get:Schema(title = "结果文件名后缀", required = false)
    var resultSuffix: String? = "_enterprise_sign",
    @get:Schema(title = "归档路径", required = false)
    var archivePath: String? = "/",
    @get:Schema(title = "主App描述文件ID", required = false)
    var mobileProvisionId: String? = null,
    @get:Schema(title = "Universal Link的设置", required = false)
    var universalLinks: List<String>? = null,
    @get:Schema(title = "安全应用组，应为securityApplicationGroupList", required = false)
    var keychainAccessGroups: List<String>? = null,
    @get:Schema(title = "是否替换bundleId", required = false)
    var replaceBundleId: Boolean? = false,
    @get:Schema(title = "拓展应用名和对应的描述文件ID", required = false)
    var appexSignInfo: List<AppexSignInfo>? = null,
    @get:Schema(title = "待替换的plist信息", required = false)
    var replaceKeyList: Map<String, String>? = null,
    @get:Schema(title = "待移除的plist key信息", required = false)
    var removeKeyList: List<String>? = null,
    @get:Schema(title = "指定xcode签名工具版本", required = false)
    var codeSignVersion: String? = null,
    @get:Schema(title = "codesign插件额外参数", required = false)
    var codesignExternalStr: String? = null,
    @get:Schema(title = "钥匙串访问组", required = false)
    var keychainAccessGroupList: List<String>? = null,
    @get:Schema(title = "更换的bundleId", required = false)
    var bundleId: String? = null,
    @get:Schema(title = "更换的bundle名", required = false)
    var bundleName: String? = null,
    @get:Schema(title = "更换的bundle版本号", required = false)
    var bundleVersion: String? = null
)
