/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.vo.redline;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 圈复杂度、重复率红线告警
 *
 * @version V1.0
 * @date 2019/7/4
 */
@Data
@ApiModel("圈复杂度红线告警")
public class RLCcnAndDupcDefectVO
{

    @ApiModelProperty("平均圈复杂度/平均代码重复率")
    private double average;

    @ApiModelProperty("单函数圈复杂度最大值")
    private long singleFuncMax;

    @ApiModelProperty("极高风险函数数量/极高风险文件数")
    private long extreme;

    @ApiModelProperty("高风险函数数量/高风险文件数")
    private long high;

    @ApiModelProperty("中风险函数数量/中风险文件数率")
    private long middle;

    @ApiModelProperty("单文件代码重复率最大值")
    private double singleFileMax;

    @ApiModelProperty("新增单函数圈复杂度最大值")
    private long newSingleFuncMax;

    @ApiModelProperty("新增风险函数数量")
    private long newFuncCount;

    @ApiModelProperty("新风险函数超标复杂度总数")
    private long newFuncBeyondThresholdSum;

    @ApiModelProperty("历史风险函数超标复杂度总数")
    private long historyFuncBeyondThresholdSum;
}
