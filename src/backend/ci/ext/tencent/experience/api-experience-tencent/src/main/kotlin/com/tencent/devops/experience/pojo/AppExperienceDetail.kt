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
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-版本详情")
data class AppExperienceDetail(
    @ApiModelProperty("版本体验ID", required = true)
    val experienceHashId: String,
    @ApiModelProperty("文件大小(byte)", required = true)
    val size: Long,
    @ApiModelProperty("logo链接", required = true)
    val logoUrl: String,
    @ApiModelProperty("分享链接", required = true)
    val shareUrl: String,
    @ApiModelProperty("版本名称", required = true)
    val name: String,
    @ApiModelProperty("包名称", required = true)
    val packageName: String,
    @ApiModelProperty("平台", required = true)
    val platform: PlatformEnum,
    @ApiModelProperty("版本体验版本号", required = true)
    val version: String,
    @ApiModelProperty("是否已过期", required = true)
    val expired: Boolean,
    @ApiModelProperty("是否可体验", required = true)
    val canExperience: Boolean,
    @ApiModelProperty("是否在线", required = true)
    val online: Boolean,
    @ApiModelProperty("是否订阅", required = true)
    val subscribe: Boolean,
    @ApiModelProperty("更新日志", required = true)
    val changeLog: List<ExperienceChangeLog>,
    @ApiModelProperty("体验名称", required = true)
    val experienceName: String,
    @ApiModelProperty("版本标题", required = true)
    val versionTitle: String,
    @ApiModelProperty("产品类别", required = true)
    val categoryId: Int,
    @ApiModelProperty("产品负责人", required = true)
    val productOwner: List<String>,
    @ApiModelProperty("创建时间", required = true)
    val createDate: Long,
    @ApiModelProperty("体验截至时间", required = true)
    val endDate: Long,
    @ApiModelProperty("是否为公开体验", required = true)
    val publicExperience: Boolean,
    @ApiModelProperty("描述", required = true)
    val remark: String,
    @ApiModelProperty("版本体验BundleIdentifier", required = true)
    val bundleIdentifier: String,
    @ApiModelProperty("体验状态", required = true)
    val experienceCondition: Int,
    @ApiModelProperty("应用Scheme", required = false)
    val appScheme: String,
    @ApiModelProperty("上次下载的体验ID", required = true)
    val lastDownloadHashId: String
)
