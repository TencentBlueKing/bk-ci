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

package com.tencent.devops.project.pojo

import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.project.pojo.enums.PluginDetailsDisplayOrder
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
    var disableWhenInactive: Boolean? = null,
    @get:Schema(title = "该项目是否开启流水线可观测数据", required = false)
    val buildMetrics: Boolean? = null,
    @get:Schema(title = "是否控制流水线列表权限", required = false)
    var pipelineListPermissionControl: Boolean? = null,
    @get:Schema(title = "插件详情展示顺序", required = false)
    var pluginDetailsDisplayOrder: List<PluginDetailsDisplayOrder>? = listOf(
        PluginDetailsDisplayOrder.LOG,
        PluginDetailsDisplayOrder.ARTIFACT,
        PluginDetailsDisplayOrder.CONFIG
    ),
    @get:Schema(title = "流水线语法风格")
    var pipelineDialect: String? = "CLASSIC",
    @get:Schema(title = "是否开启流水线命名提示")
    var enablePipelineNameTips: Boolean? = false,
    @get:Schema(title = "流水线命名格式")
    var pipelineNameFormat: String? = null,
    @get:Schema(title = "构建日志归档阈值(单位:万)")
    var loggingLineLimit: Int? = null,
    @get:Schema(title = "是否启用观察员模式", required = false)
    val remotedevObserver: Boolean? = null
) {
    /**
     * 接受前端请求时,只复制前端展示修改的值,由op控制的值不能修改
     */
    fun userCopy(updateProperties: ProjectProperties): ProjectProperties {
        with(updateProperties) {
            return this@ProjectProperties.copy(
                pipelineDialect = pipelineDialect,
                enablePipelineNameTips = enablePipelineNameTips,
                pipelineNameFormat = pipelineNameFormat,
                loggingLineLimit = loggingLineLimit
            )
        }
    }
}
