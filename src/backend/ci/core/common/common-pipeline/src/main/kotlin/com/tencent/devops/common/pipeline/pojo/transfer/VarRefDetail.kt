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

package com.tencent.devops.common.pipeline.pojo.transfer

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "变量引用详情")
data class VarRefDetail(
    @Schema(title = "主键ID")
    val id: Long? = null,
    @Schema(title = "项目ID")
    val projectId: String,
    @Schema(title = "变量名称")
    val varName: String,
    @Schema(title = "关联资源ID")
    val resourceId: String = "",
    @Schema(title = "关联资源类型")
    val resourceType: String,
    @Schema(title = "关联资源版本名称")
    val resourceVersionName: String? = "",
    @Schema(title = "关联资源版本号")
    val referVersion: Int = 1,
    @Schema(title = "步骤ID")
    val stageId: String = "",
    @Schema(title = "构建容器ID")
    val containerId: String? = null,
    @Schema(title = "任务ID")
    val taskId: String? = null,
    @Schema(title = "引用变量的参数路径")
    val positionPath: String,
    @Schema(title = "创建者")
    val creator: String = "system",
    @Schema(title = "修改者")
    val modifier: String = "system",
    @Schema(title = "修改时间")
    val updateTime: LocalDateTime? = null,
    @Schema(title = "创建时间")
    val createTime: LocalDateTime? = null
)
