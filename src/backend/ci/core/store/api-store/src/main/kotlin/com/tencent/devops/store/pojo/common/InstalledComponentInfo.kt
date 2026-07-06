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

package com.tencent.devops.store.pojo.common

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nSourceEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "已安装组件信息")
data class InstalledComponentInfo(
    @get:Schema(title = "组件ID")
    val storeId: String,
    @get:Schema(title = "组件标识")
    val storeCode: String,
    @get:Schema(title = "组件名称")
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val storeName: String,
    @get:Schema(title = "组件类型")
    val storeType: StoreTypeEnum,
    @get:Schema(title = "项目下已安装的版本号")
    val installedVersion: String?,
    @get:Schema(title = "logo地址")
    val logoUrl: String?,
    @get:Schema(title = "分类标识")
    val classifyCode: String?,
    @get:Schema(title = "分类名称")
    val classifyName: String?,
    @get:Schema(title = "发布者")
    @BkFieldI18n(source = I18nSourceEnum.DB, keyPrefixName = "versionInfo")
    val publisher: String?,
    @get:Schema(title = "安装者")
    val installer: String?,
    @get:Schema(title = "安装时间")
    val installTime: String?,
    @get:Schema(title = "安装类型(INIT:初始化项目 COMMON:安装项目 TEST:调试项目)")
    val installType: String?,
    @get:Schema(title = "实例ID")
    val instanceId: String?,
    @get:Schema(title = "实例名称")
    val instanceName: String?,
    @get:Schema(title = "组件类型个性化扩展信息(由各storeType按需填充，如插件关联流水线数、卸载权限等)")
    val extData: MutableMap<String, Any?> = mutableMapOf()
)
