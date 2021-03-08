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

@ApiModel("store组件环境变量信息")
data class StoreEnvVarInfo(
    @ApiModelProperty("环境变量ID", required = true)
    val id: String,
    @ApiModelProperty("store组件代码", required = true)
    val storeCode: String,
    @ApiModelProperty("store组件类型", required = true)
    val storeType: String,
    @ApiModelProperty("变量名", required = true)
    val varName: String,
    @ApiModelProperty("变量值", required = true)
    val varValue: String,
    @ApiModelProperty("描述", required = false)
    val varDesc: String?,
    @ApiModelProperty("变量值是否加密", required = true)
    val encryptFlag: Boolean,
    @ApiModelProperty("适用范围 TEST：测试 PRD：正式 ALL：所有", required = true)
    val scope: String,
    @ApiModelProperty("版本号", required = true)
    val version: Int,
    @ApiModelProperty("添加用户", required = true)
    val creator: String,
    @ApiModelProperty("修改用户", required = true)
    val modifier: String,
    @ApiModelProperty("添加时间", required = true)
    val createTime: String,
    @ApiModelProperty("修改时间", required = true)
    val updateTime: String
)
