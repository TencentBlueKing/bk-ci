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
import com.tencent.devops.experience.pojo.enums.Source
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-发布摘要")
data class ExperienceSummaryWithPermission(
    @ApiModelProperty("发布HashId", required = true)
    val experienceHashId: String,
    @ApiModelProperty("版本名称", required = true)
    val name: String,
    @ApiModelProperty("平台", required = true)
    val platform: PlatformEnum,
    @ApiModelProperty("版本号", required = true)
    val version: String,
    @ApiModelProperty("描述", required = false)
    val remark: String?,
    @ApiModelProperty("截止日期", required = true)
    val expireDate: Long,
    @ApiModelProperty("来源", required = true)
    val source: Source,
    @ApiModelProperty("创建者", required = true)
    val creator: String,
    @ApiModelProperty("是否已过期", required = true)
    val expired: Boolean,
    @ApiModelProperty("是否在线", required = true)
    val online: Boolean,
    @ApiModelProperty("权限", required = true)
    val permissions: ExperiencePermission
)
