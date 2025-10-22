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

package com.tencent.devops.store.pojo.atom

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nSourceEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "已安装插件")
data class InstalledAtom(
    @get:Schema(title = "插件ID")
    val atomId: String,
    @get:Schema(title = "插件标识")
    val atomCode: String,
    @get:Schema(title = "插件版本")
    val version: String,
    @get:Schema(title = "插件名称")
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val name: String,
    @get:Schema(title = "logo地址")
    val logoUrl: String?,
    @get:Schema(title = "插件分类code")
    val classifyCode: String?,
    @get:Schema(title = "插件分类名称")
    val classifyName: String?,
    @get:Schema(title = "插件范畴")
    val category: String?,
    @get:Schema(title = "插件简介")
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val summary: String?,
    @get:Schema(title = "发布者")
    @BkFieldI18n(source = I18nSourceEnum.DB, keyPrefixName = "versionInfo")
    val publisher: String?,
    @get:Schema(title = "安装者")
    val installer: String,
    @get:Schema(title = "安装时间")
    val installTime: String,
    @get:Schema(title = "安装类型")
    val installType: String,
    @get:Schema(title = "流水线个数")
    val pipelineCnt: Int,
    @get:Schema(title = "是否有卸载权限")
    val hasPermission: Boolean
)
