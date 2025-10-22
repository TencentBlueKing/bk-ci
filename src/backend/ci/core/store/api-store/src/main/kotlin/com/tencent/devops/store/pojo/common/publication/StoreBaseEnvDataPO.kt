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
import java.time.LocalDateTime

@Schema(title = "组件基本环境信息数据PO")
data class StoreBaseEnvDataPO(
    @get:Schema(title = "主键ID")
    val id: String,
    @get:Schema(title = "组件ID")
    val storeId: String,
    @get:Schema(title = "开发语言")
    val language: String? = null,
    @get:Schema(title = "支持开发语言的最低版本")
    val minVersion: String? = null,
    @get:Schema(title = "包名称")
    val pkgName: String? = null,
    @get:Schema(title = "包路径")
    val pkgPath: String? = null,
    @get:Schema(title = "执行入口命令")
    val target: String? = null,
    @get:Schema(title = "SHA签名串")
    val shaContent: String? = null,
    @get:Schema(title = "执行前置命令")
    val preCmd: String? = null,
    @get:Schema(title = "支持的操作系统名称")
    val osName: String? = null,
    @get:Schema(title = "支持的操作系统架构")
    val osArch: String? = null,
    @get:Schema(title = "SHA签名串")
    val runtimeVersion: String? = null,
    @get:Schema(title = "是否为默认环境信息")
    val defaultFlag: Boolean? = null,
    @get:Schema(title = "创建人")
    val creator: String,
    @get:Schema(title = "修改人")
    val modifier: String,
    @get:Schema(title = "创建时间")
    val createTime: LocalDateTime = LocalDateTime.now(),
    @get:Schema(title = "更新时间")
    val updateTime: LocalDateTime = LocalDateTime.now()
)
