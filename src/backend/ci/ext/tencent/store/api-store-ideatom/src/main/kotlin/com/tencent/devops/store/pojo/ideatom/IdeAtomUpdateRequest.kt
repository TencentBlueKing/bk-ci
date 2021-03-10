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

package com.tencent.devops.store.pojo.ideatom

import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("更新IDE插件请求报文体")
data class IdeAtomUpdateRequest(
    @ApiModelProperty("插件名称", required = false)
    val atomName: String?,
    @ApiModelProperty("插件类型，SELF_DEVELOPED：自研 THIRD_PARTY：第三方开发", required = false)
    val atomType: IdeAtomTypeEnum?,
    @ApiModelProperty("所属分类代码", required = false)
    val classifyCode: String?,
    @ApiModelProperty("版本日志内容", required = false)
    val versionContent: String?,
    @ApiModelProperty("插件logo地址", required = false)
    val logoUrl: String?,
    @ApiModelProperty("插件简介", required = false)
    val summary: String?,
    @ApiModelProperty("插件描述", required = false)
    val description: String?,
    @ApiModelProperty("发布者", required = false)
    val publisher: String?,
    @ApiModelProperty("是否为公共插件 true：公共插件 false：普通插件", required = false)
    val publicFlag: Boolean?,
    @ApiModelProperty("是否推荐， TRUE：是 FALSE：不是", required = false)
    val recommendFlag: Boolean?,
    @ApiModelProperty("权重（数值越大代表权重越高）", required = false)
    val weight: Int?,
    @ApiModelProperty("插件标签列表", required = false)
    val labelIdList: ArrayList<String>?,
    @ApiModelProperty("应用范畴列表", required = false)
    val categoryIdList: ArrayList<String>?,
    @ApiModelProperty(value = "插件项目可视范围", required = false)
    val visibilityLevel: VisibilityLevelEnum?,
    @ApiModelProperty(value = "插件代码库不开源原因", required = false)
    val privateReason: String?
)
