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

package com.tencent.devops.project.api.pojo

import com.tencent.devops.project.api.pojo.enums.HtmlComponentTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("扩展服务功能点信息")
data class ServiceItemInfoVO(
    @ApiModelProperty("扩展点ID", required = true)
    val itemId: String,
    @ApiModelProperty("扩展点标识", required = true)
    val itemCode: String,
    @ApiModelProperty("扩展点名称", required = true)
    val itemName: String,
    @ApiModelProperty("扩展点对应的页面路径信息", required = true)
    val htmlPath: String,
    @ApiModelProperty("扩展点对应的前端组件类型", required = true)
    val htmlComponentType: HtmlComponentTypeEnum,
    @ApiModelProperty("扩展点提示信息", required = false)
    val tooltip: String?,
    @ApiModelProperty("扩展点对应的图标地址", required = false)
    val iconUrl: String?,
    @ApiModelProperty("自定义扩展点前端表单属性配置Json串", required = false)
    val props: Map<String, Any>?
)
