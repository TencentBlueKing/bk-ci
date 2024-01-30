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

package com.tencent.devops.store.pojo.common

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("发布者机构信息报文体")
data class PublisherDeptInfo(
    @ApiModelProperty("发布者标识")
    val publisherCode: String,
    @ApiModelProperty("一级部门ID")
    val firstLevelDeptId: Long,
    @ApiModelProperty("一级部门名称")
    val firstLevelDeptName: String,
    @ApiModelProperty("二级部门ID")
    val secondLevelDeptId: Long,
    @ApiModelProperty("二级部门名称")
    val secondLevelDeptName: String,
    @ApiModelProperty("三级部门ID")
    val thirdLevelDeptId: Long,
    @ApiModelProperty("三级部门名称")
    val thirdLevelDeptName: String,
    @ApiModelProperty("四级部门ID")
    val fourthLevelDeptId: Long? = null,
    @ApiModelProperty("四级部门名称")
    val fourthLevelDeptName: String? = null,
    @ApiModelProperty("实体组织架构")
    var organizationName: String = "",
    @ApiModelProperty("所属工作组BG")
    val bgName: String
)
