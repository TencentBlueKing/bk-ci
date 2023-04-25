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

import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("IDE插件市场首页单项信息")
data class MarketIdeAtomItem(
    @ApiModelProperty("插件ID", required = true)
    val id: String,
    @ApiModelProperty("插件名称", required = true)
    val name: String,
    @ApiModelProperty("插件代码", required = true)
    val code: String,
    @ApiModelProperty("插件类型，SELF_DEVELOPED：自研 THIRD_PARTY：第三方开发", required = true)
    val rdType: IdeAtomTypeEnum?,
    @ApiModelProperty("所属分类代码", required = true)
    val classifyCode: String?,
    @ApiModelProperty("插件logo地址", required = false)
    val logoUrl: String?,
    @ApiModelProperty("插件简介", required = false)
    val summary: String?,
    @ApiModelProperty("发布者", required = true)
    val publisher: String,
    @ApiModelProperty("发布时间", required = true)
    val pubTime: String?,
    @ApiModelProperty("是否为最新版本插件 true：最新 false：非最新", required = true)
    val latestFlag: Boolean,
    @ApiModelProperty("是否为公共插件 true：公共插件 false：普通插件", required = true)
    val publicFlag: Boolean,
    @ApiModelProperty("是否推荐， TRUE：是 FALSE：不是", required = true)
    val recommendFlag: Boolean,
    @ApiModelProperty("是否有权限安装标识")
    val flag: Boolean,
    @ApiModelProperty("创建人", required = true)
    val creator: String,
    @ApiModelProperty("创建时间", required = true)
    val createTime: String,
    @ApiModelProperty("修改人", required = true)
    val modifier: String,
    @ApiModelProperty("修改时间", required = true)
    val updateTime: String,
    @ApiModelProperty("下载量")
    val downloads: Int?,
    @ApiModelProperty("评分")
    val score: Double?,
    @ApiModelProperty("权重")
    val weight: Int?
)
