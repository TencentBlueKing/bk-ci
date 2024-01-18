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

import com.tencent.devops.common.api.enums.PlatformEnum
import com.tencent.devops.experience.pojo.enums.ArtifactoryType
import com.tencent.devops.experience.pojo.group.Group
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "版本体验-发布信息")
data class Experience(
    @get:Schema(title = "版本名称", required = true)
    val name: String,
    @get:Schema(title = "文件路径", required = true)
    val path: String,
    @get:Schema(title = "版本仓库类型", required = true)
    val artifactoryType: ArtifactoryType,
    @get:Schema(title = "平台", required = true)
    val platform: PlatformEnum?,
    @get:Schema(title = "版本号", required = true)
    val version: String,
    @get:Schema(title = "描述", required = false)
    val remark: String?,
    @get:Schema(title = "创建时间", required = true)
    val createDate: Long,
    @get:Schema(title = "截止日期", required = true)
    val expireDate: Long,
    @get:Schema(title = "体验组", required = true)
    val experienceGroups: List<Group>,
    @get:Schema(title = "内部名单", required = true)
    val innerUsers: Set<String>,
    @get:Schema(title = "外部名单", required = true)
    val outerUsers: Set<String>,
    @get:Schema(title = "通知类型", required = true)
    val notifyTypes: Set<NotifyType>,
    @get:Schema(title = "是否开启企业微信群", required = true)
    val enableWechatGroups: Boolean,
    @get:Schema(title = "企业微信群ID(逗号分隔)", required = false)
    val wechatGroups: String,
    @get:Schema(title = "创建者", required = true)
    val creator: String,
    @get:Schema(title = "是否已过期", required = true)
    val expired: Boolean,
    @get:Schema(title = "是否可体验", required = true)
    val canExperience: Boolean,
    @get:Schema(title = "是否在线", required = true)
    val online: Boolean,
    @get:Schema(title = "下载链接", required = true)
    val url: String?,
    @get:Schema(title = "体验名称", required = true)
    val experienceName: String,
    @get:Schema(title = "版本标题", required = true)
    val versionTitle: String,
    @get:Schema(title = "产品类别", required = true)
    val categoryId: Int,
    @get:Schema(title = "产品负责人", required = true)
    val productOwner: List<String>
)
