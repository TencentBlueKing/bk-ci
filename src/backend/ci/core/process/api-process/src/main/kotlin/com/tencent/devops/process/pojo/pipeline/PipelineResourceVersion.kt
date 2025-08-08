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

package com.tencent.devops.process.pojo.pipeline

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.process.pojo.setting.PipelineVersionSimple
import com.tencent.devops.process.utils.PipelineVersionUtils
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Suppress("LongParameterList", "LongMethod")
@Schema(title = "流水线版本-详细内容")
data class PipelineResourceVersion(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "记录版本号", required = true)
    val version: Int,
    @get:Schema(title = "JSON编排内容（POJO）", required = true)
    var model: Model,
    @get:Schema(title = "YAML编排内容", required = false)
    var yaml: String?,
    @get:Schema(title = "YAML编排版本", required = false)
    var yamlVersion: String?,
    @get:Schema(title = "版本名称", required = true)
    val versionName: String?,
    @get:Schema(title = "创建者", required = true)
    val creator: String,
    @get:Schema(title = "版本创建时间", required = true)
    val createTime: LocalDateTime,
    @get:Schema(title = "更新操作人", required = true)
    val updater: String?,
    @get:Schema(title = "版本修改时间", required = true)
    val updateTime: LocalDateTime?,
    @get:Schema(title = "发布版本号", required = false)
    val versionNum: Int?,
    @get:Schema(title = "编排版本号", required = false)
    val pipelineVersion: Int?,
    @get:Schema(title = "触发器版本号", required = false)
    val triggerVersion: Int?,
    @get:Schema(title = "设置版本号", required = false)
    val settingVersion: Int?,
    @get:Schema(title = "是否还有构建记录引用该版本标识", required = false)
    val referFlag: Boolean? = null,
    @get:Schema(title = "关联构建记录总数", required = false)
    val referCount: Int? = null,
    @get:Schema(title = "草稿版本标识", required = false)
    val status: VersionStatus? = VersionStatus.RELEASED,
    @get:Schema(title = "分支版本状态", required = false)
    val branchAction: BranchVersionAction? = null,
    @get:Schema(title = "版本变更说明", required = false)
    val description: String? = null,
    @get:Schema(title = "调试构建ID", required = false)
    val debugBuildId: String? = null,
    @get:Schema(title = "该版本的来源版本（空时一定为主路径）", required = false)
    val baseVersion: Int? = null
) {
    fun toSimple() = PipelineVersionSimple(
        pipelineId = pipelineId,
        creator = creator,
        createTime = createTime.timestampmilli(),
        updater = updater,
        updateTime = updateTime?.timestampmilli(),
        version = version,
        versionName = versionName ?: PipelineVersionUtils.getVersionName(
            versionNum = version,
            pipelineVersion = versionNum ?: version,
            triggerVersion = 0,
            settingVersion = 0
        ) ?: "",
        referFlag = referFlag,
        referCount = referCount,
        versionNum = versionNum,
        pipelineVersion = pipelineVersion,
        triggerVersion = triggerVersion,
        settingVersion = settingVersion,
        status = status,
        debugBuildId = debugBuildId,
        baseVersion = baseVersion,
        description = description,
        yamlVersion = yamlVersion
    )
}
