/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 重复率统计视图
 * 
 * @date 2019/12/3
 * @version V1.0
 */
@Data
@ApiModel("重复率统计视图")
public class DUPCStatisticVO extends StatisticVO
{

    /**
     * 本次分析前的遗留告警数
     */
    @ApiModelProperty("本次分析前的遗留告警数")
    private int lastDefectCount;

    /**
     * 本次分析的代码重复率
     */
    @ApiModelProperty("本次分析的代码重复率")
    private float dupRate;

    /**
     * 本次分析前的代码重复率
     */
    @ApiModelProperty("本次分析前的代码重复率")
    private float lastDupRate;

    /**
     * 本次分析的代码重复率变化值
     */
    @ApiModelProperty("本次分析的代码重复率变化值")
    private float dupRateChange;

    /**
     * 代码重复率工具的扫描统计结果
     */
    @ApiModelProperty("代码重复率工具的扫描统计结果")
    private DUPCScanSummaryVO dupcScanSummary;


}
