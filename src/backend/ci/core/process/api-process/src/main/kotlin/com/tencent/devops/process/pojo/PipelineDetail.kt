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

package com.tencent.devops.process.pojo

import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线预览页完整信息")
data class PipelineDetail(
    @get:Schema(title = "流水线Id")
    val pipelineId: String,
    @get:Schema(title = "流水线名称")
    val pipelineName: String,
    @get:Schema(title = "是否收藏")
    val hasCollect: Boolean,
    @get:Schema(title = "是否可以手动触发")
    val canManualStartup: Boolean,
    @get:Schema(title = "是否可以调试")
    val canDebug: Boolean,
    @get:Schema(title = "是否可以发布")
    val canRelease: Boolean,
    @get:Schema(title = "是否从模板实例化")
    val instanceFromTemplate: Boolean,
    @get:Schema(title = "当前模板的ID", required = false)
    var templateId: String?,
    @get:Schema(title = "关联模板版本", required = false)
    var templateVersion: Long?,
    @get:Schema(title = "草稿或最新的发布版本")
    val version: Int,
    @get:Schema(title = "草稿或最新的发布版本名称")
    val versionName: String?,
    @get:Schema(title = "草稿的基准版本（存在草稿才有值）", required = false)
    val baseVersion: Int?,
    @get:Schema(title = "草稿的基准版本的状态（存在草稿才有值）", required = false)
    val baseVersionStatus: VersionStatus?,
    @get:Schema(title = "基准版本的版本名称")
    val baseVersionName: String?,
    @get:Schema(title = "最新的发布版本，如果为空则说明没有过发布版本")
    val releaseVersion: Int?,
    @get:Schema(title = "最新的发布版本名称，如果为空则说明没有过发布版本")
    val releaseVersionName: String?,
    @get:Schema(title = "是否有编辑权限")
    val hasPermission: Boolean,
    @get:Schema(title = "流水线描述")
    val pipelineDesc: String,
    @get:Schema(title = "创建者")
    val creator: String,
    @get:Schema(title = "创建时间")
    val createTime: Long = 0,
    @get:Schema(title = "更新时间")
    val updateTime: Long = 0,
    @get:Schema(title = "流水线组名称列表", required = false)
    var viewNames: List<String>?,
    @get:Schema(title = "流水线权限", required = false)
    val permissions: PipelinePermissions?,
    @get:Schema(title = "Lock 类型", required = false)
    val runLockType: PipelineRunLockType? = null,
    @get:Schema(title = "最新流水线版本状态（如有任何发布版本则为发布版本）", required = false)
    var latestVersionStatus: VersionStatus? = VersionStatus.RELEASED,
    @get:Schema(title = "PAC配置", required = false)
    val pipelineAsCodeSettings: PipelineAsCodeSettings?,
    @get:Schema(title = "流水线YAML信息", required = false)
    val yamlInfo: PipelineYamlVo?,
    @get:Schema(title = "yaml文件在默认分支是否存在", required = false)
    var yamlExist: Boolean? = false,
    @get:Schema(title = "运行锁定", required = false)
    val locked: Boolean
)
