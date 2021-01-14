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

package com.tencent.bk.codecc.defect.model.pipelinereport;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.codecc.defect.model.ChartAverageEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 圈复杂度工具快照实体类
 *
 * @version V1.0
 * @date 2019/6/29
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CCNSnapShotEntity extends ToolSnapShotEntity
{

    /**
     * 最近一次分析，风险函数总个数
     */
    @Field("total_risk_func_count")
    @JsonProperty("total_risk_func_count")
    private int totalRiskFuncCount;

    /**
     * 最近一次分析，风险函数变化个数
     */
    @Field("changed_risk_func_count")
    @JsonProperty("changed_risk_func_count")
    private int changeedRiskFuncCount;

    /**
     * 最近一次分析，平均圈复杂度
     */
    @Field("average_ccn")
    @JsonProperty("average_ccn")
    private String averageCcn;

    /**
     * 最近一次分析，变化的圈复杂度
     */
    @Field("changed_ccn")
    @JsonProperty("changed_ccn")
    private String changedCcn;

    /**
     * 风险函数级别分布
     */
    @Field("super_high")
    @JsonProperty("super_high")
    private int superHigh;

    private int high;

    private int medium;

    private int low;

    /**
     * 平均圈复杂度趋势
     */
    @Field("average_ccn_chart")
    @JsonProperty("average_ccn_chart")
    private List<ChartAverageEntity> averageCcnChart;

    /**
     * 风险函数超标圈复杂度总和
     */
    @Field("ccn_beyond_threshold_sum")
    @JsonProperty("ccn_beyond_threshold_sum")
    private Integer ccnBeyondThresholdSum;

}
