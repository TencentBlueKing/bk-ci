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

import com.tencent.devops.store.pojo.common.enums.PublisherType
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("发布者数据同步请求")
data class PublishersRequest(
    @ApiModelProperty("发布者标识", required = true)
    val publishersCode: String,
    @ApiModelProperty("发布者名称", required = true)
    val name: String,
    @ApiModelProperty("发布者类型", required = true)
    val publishersType: PublisherType,
    @ApiModelProperty("主体负责人", required = true)
    val owners: List<String>,
    @ApiModelProperty("成员", required = true)
    val members: List<String>,
    @ApiModelProperty("技术支持", required = false)
    val helper: String? = null,
    @ApiModelProperty("是否认证", required = true)
    val certificationFlag: Boolean,
    @ApiModelProperty("组件类型", required = true)
    val storeType: StoreTypeEnum,
    @ApiModelProperty("实体组织架构", required = true)
    val organization: String,
    @ApiModelProperty("所属工作组BG", required = true)
    val bgName: String
)
