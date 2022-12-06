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
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-创建发布信息")
data class ExperienceServiceCreate(
    @ApiModelProperty("文件路径", required = true)
    val path: String,
    @ApiModelProperty("版本仓库类型", required = true)
    val artifactoryType: ArtifactoryType,
    @ApiModelProperty("截止日期", required = true)
    val expireDate: Long,
    @ApiModelProperty("体验组", required = false)
    val experienceGroups: Set<String> = setOf(),
    @ApiModelProperty("内部名单", required = false)
    val innerUsers: Set<String> = setOf(),
    @ApiModelProperty("外部名单", required = false)
    val outerUsers: Set<String> = setOf(),
    @ApiModelProperty("通知类型", required = false)
    val notifyTypes: Set<NotifyType> = setOf(),
    @ApiModelProperty("是否开启企业微信群", required = true)
    val enableWechatGroups: Boolean = true,
    @ApiModelProperty("企业微信群", required = false)
    val wechatGroups: String = "",
    @ApiModelProperty("版本体验描述", required = false)
    val description: String? = "",
    @ApiModelProperty("体验名称", required = true)
    val experienceName: String?,
    @ApiModelProperty("版本标题", required = true)
    val versionTitle: String?,
    @ApiModelProperty("产品类别", required = true)
    val categoryId: Int?,
    @ApiModelProperty("产品负责人", required = true)
    val productOwner: List<String>?

)
