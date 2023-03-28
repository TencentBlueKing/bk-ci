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

package com.tencent.devops.store.pojo.common.index

import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.store.pojo.common.enums.IndexExecuteTimeTypeEnum
import com.tencent.devops.store.pojo.common.enums.IndexOperationTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("新增指标请求报文体")
data class StoreIndexCreateRequest(
    @ApiModelProperty("指标代码", required = true)
    @BkField(maxLength = 10, patternStyle = BkStyleEnum.CODE_STYLE)
    val indexCode: String,
    @ApiModelProperty("指标名称", required = true)
    @BkField(maxLength = 64)
    val indexName: String,
    @ApiModelProperty("指标描述", required = true)
    @BkField(maxLength = 256)
    val description: String,
    @ApiModelProperty("等级信息", required = true)
    val levelInfos: List<StoreIndexLevelInfo>,
    @ApiModelProperty("运算类型", required = true)
    val operationType: IndexOperationTypeEnum,
    @ApiModelProperty("指标对应的插件件代码", required = false)
    val atomCode: String? = null,
    @ApiModelProperty("指标对应的插件版本", required = false)
    val atomVersion: String? = null,
    @ApiModelProperty("指标执行时机类型", required = true)
    val executeTimeType: IndexExecuteTimeTypeEnum,
    @ApiModelProperty("store组件类型", required = true)
    val storeType: StoreTypeEnum,
    @ApiModelProperty("指标展示权重", required = true)
    val weight: Int
)
