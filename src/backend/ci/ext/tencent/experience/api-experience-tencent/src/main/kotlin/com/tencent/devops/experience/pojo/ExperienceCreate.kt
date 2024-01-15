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

package com.tencent.devops.experience.pojo

import com.tencent.devops.experience.pojo.enums.ArtifactoryType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "版本体验-创建发布信息")
data class ExperienceCreate(
    @Schema(description = "版本名称", required = true)
    val name: String,
    @Schema(description = "文件路径", required = true)
    val path: String,
    @Schema(description = "版本仓库类型", required = true)
    val artifactoryType: ArtifactoryType,
    @Schema(description = "描述", required = false)
    val remark: String?,
    @Schema(description = "截止日期", required = true)
    val expireDate: Long,
    @Schema(description = "体验组", required = true)
    val experienceGroups: Set<String>,
    @Schema(description = "内部名单", required = true)
    val innerUsers: Set<String>,
    @Schema(description = "外部名单", required = true)
    val outerUsers: Set<String>,
    @Schema(description = "通知类型", required = true)
    val notifyTypes: Set<NotifyType>,
    @Schema(description = "是否开启企业微信群", required = true)
    val enableWechatGroups: Boolean = true,
    @Schema(description = "企业微信群ID(逗号分隔)", required = false)
    val wechatGroups: String?,
    @Schema(description = "体验名称", required = true)
    var experienceName: String?,
    @Schema(description = "版本标题", required = true)
    val versionTitle: String?,
    @Schema(description = "产品类别", required = true)
    val categoryId: Int?,
    @Schema(description = "产品负责人", required = true)
    val productOwner: List<String>?,
    @Schema(description = "体验范围,0--公开体验 , 1--内部体验", required = false)
    val groupScope: Int? = null,
    @Schema(description = "是否发送通知", required = false)
    val sendNotification: Boolean = true
)
