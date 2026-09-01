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

package com.tencent.devops.store.pojo.common.deploy

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nSourceEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户可拉取组件部署信息")
data class UserComponentDeployInfo(
    @get:Schema(title = "组件ID", required = true)
    val storeId: String,
    @get:Schema(title = "组件标识", required = true)
    val storeCode: String,
    @get:Schema(title = "组件类型", required = true)
    val storeType: String,
    @get:Schema(title = "用户配置的应用名", required = true)
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val name: String,
    @get:Schema(title = "是否有权限安装该应用", required = true)
    val installFlag: Boolean,
    @get:Schema(title = "最新版本号", required = true)
    val latestVersion: String,
    @get:Schema(
        title = "组件级共享扩展字段集合(所有版本共享，如DEVX组件的安装路径installPath，可空)",
        required = false
    )
    val extData: Map<String, Any>? = null,
    @get:Schema(title = "版本信息列表", required = true)
    val versionInfos: List<ComponentDeployVersionInfo>
)

@Schema(title = "组件部署版本信息")
data class ComponentDeployVersionInfo(
    @get:Schema(title = "组件ID", required = true)
    val storeId: String,
    @get:Schema(title = "版本号", required = true)
    val version: String,
    @get:Schema(title = "是否为最新版本", required = true)
    val latestFlag: Boolean,
    @get:Schema(title = "组件状态", required = false)
    val status: String? = null,
    @get:Schema(
        title = "版本级扩展字段集合(跟随版本，如DEVX组件的安装方式installType、安装参数installParams)",
        required = false
    )
    val extData: Map<String, Any>? = null
)
