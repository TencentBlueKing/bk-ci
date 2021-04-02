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

import io.swagger.annotations.ApiModelProperty

data class ServiceItem(
    @ApiModelProperty("扩展Id")
    val itemId: String,
    @ApiModelProperty("扩展名称")
    val itemName: String,
    @ApiModelProperty("扩展标示编码")
    val itemCode: String,
    @ApiModelProperty("扩展蓝盾服务Id")
    val parentId: String,
    @ApiModelProperty("扩展蓝盾服务Name")
    var parentName: String? = "",
    @ApiModelProperty("页面路径")
    val htmlPath: String? = null,
    @ApiModelProperty("UI组件类型")
    val htmlType: String? = null,
    @ApiModelProperty("扩展服务安装个数")
    val serviceCount: Int? = 0,
    val tooltip: String? = "",
    @ApiModelProperty("icon路径")
    val icon: String? = "",
    @ApiModelProperty("props参数")
    val props: String = "",
    @ApiModelProperty("扩展点状态")
    val itemStatus: String? = "ENABLE"
)
