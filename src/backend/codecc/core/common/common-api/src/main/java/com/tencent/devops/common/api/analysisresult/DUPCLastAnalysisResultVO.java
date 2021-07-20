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

package com.tencent.devops.common.api.analysisresult;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * DUPC最近一次分析结果
 *
 * @version V1.0
 * @date 2019/6/9
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("CCN类工具最近一次分析结果")
public class DUPCLastAnalysisResultVO extends BaseRiskAnalysisResultVO {
    /**
     * 本次分析的代码重复率(平均重复率)
     */
    @ApiModelProperty("本次分析的代码重复率")
    private Float dupRate;

    /**
     * 本次分析前的代码重复率
     */
    @ApiModelProperty("本次分析前的代码重复率")
    private Float lastDupRate;

    /**
     * 本次分析的代码重复率变化值
     */
    @ApiModelProperty("本次分析的代码重复率变化值")
    private Float dupRateChange;


    /**
     * 新告警处理人统计
     */
    @ApiModelProperty("新告警处理人统计")
    private List<BaseRiskNotRepairedAuthorVO> newAuthorStatistic;

    /**
     * 存量告警处理人统计
     */
    @ApiModelProperty("存量告警处理人统计")
    private List<BaseRiskNotRepairedAuthorVO> existAuthorStatistic;
}
