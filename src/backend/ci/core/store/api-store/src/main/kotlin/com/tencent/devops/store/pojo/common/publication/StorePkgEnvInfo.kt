/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.store.pojo.common.publication

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "组件包环境信息")
data class StorePkgEnvInfo(
    @get:Schema(title = "包名", required = false)
    var pkgName: String? = "",
    @get:Schema(title = "安装包位于本地的路径", required = false)
    var pkgLocalPath: String? = "",
    @get:Schema(title = "安装包位于仓库的路径", required = true)
    var pkgRepoPath: String = "",
    @get:Schema(title = "开发语言", required = false)
    val language: String? = null,
    @get:Schema(title = "支持开发语言的最低版本", required = false)
    val minVersion: String? = null,
    @get:Schema(title = "执行入口", required = false)
    val target: String? = "",
    @get:Schema(title = "SHA签名串", required = false)
    var shaContent: String? = null,
    @get:Schema(title = "执行前置命令", required = false)
    var preCmd: String? = null,
    @get:Schema(title = "支持的操作系统名称", required = false)
    var osName: String? = null,
    @get:Schema(title = "支持的操作系统架构", required = false)
    var osArch: String? = null,
    @get:Schema(title = "运行时版本", required = false)
    val runtimeVersion: String? = null,
    @get:Schema(title = "是否为默认环境信息", required = false)
    val defaultFlag: Boolean? = null,
    @get:Schema(title = "环境扩展信息", required = false)
    val extEnvInfo: Map<String, Any>? = null
)
