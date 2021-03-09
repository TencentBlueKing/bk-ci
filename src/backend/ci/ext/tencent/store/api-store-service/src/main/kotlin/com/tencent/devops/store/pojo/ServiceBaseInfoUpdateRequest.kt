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

package com.tencent.devops.store.pojo

import com.tencent.devops.store.pojo.enums.DescInputTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("扩展服务基本信息修改请求报文体")
data class ServiceBaseInfoUpdateRequest(
    @ApiModelProperty("扩展名称", required = false)
    val serviceName: String? = null,
    @ApiModelProperty("所属分类代码", required = false)
    val classifyCode: String? = null,
    @ApiModelProperty("插件简介", required = false)
    val summary: String? = null,
    @ApiModelProperty("扩展描述", required = false)
    val description: String? = null,
    @ApiModelProperty("扩展logo", required = false)
    val logoUrl: String? = null,
    @ApiModelProperty("icon图标base64字符串", required = false)
    val iconData: String? = null,
    @ApiModelProperty("发布者", required = false)
    val publisher: String? = null,
    @ApiModelProperty("原子标签列表", required = false)
    val labelIdList: ArrayList<String>? = null,
    @ApiModelProperty("扩展点列表", required = false)
    val extensionItemIdList: Set<String>? = null,
    @ApiModelProperty("媒体信息列表", required = false)
    val mediaList: List<UpdateMediaInfo>? = null,
    @ApiModelProperty("描述录入类型")
    val descInputType: DescInputTypeEnum? = DescInputTypeEnum.MANUAL
)
