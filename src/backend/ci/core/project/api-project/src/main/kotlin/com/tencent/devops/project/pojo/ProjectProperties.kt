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

package com.tencent.devops.project.pojo

import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目其他配置")
data class ProjectProperties(
    @get:Schema(title = "YAML流水线功能设置")
    val pipelineAsCodeSettings: PipelineAsCodeSettings = PipelineAsCodeSettings(
        enable = false
    ),
    @get:Schema(title = "是否启用云研发", required = false)
    val remotedev: Boolean? = false,
    @get:Schema(title = "可申请的云桌面数", required = false)
    val cloudDesktopNum: Int = 0,
    @get:Schema(title = "云研发管理员，多人用分号分隔", required = false)
    val remotedevManager: String? = null,
    @get:Schema(title = "是否开启流水线模板管理", required = false)
    var enableTemplatePermissionManage: Boolean? = null,
    @get:Schema(title = "数据标签，创建项目时会为该项目分配指定标签的db")
    val dataTag: String? = null,
    @get:Schema(title = "当项目不活跃时，是否禁用")
    var disableWhenInactive: Boolean? = null
)
