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

package com.tencent.bk.codecc.apiquery.defect.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 每次分析结束的统计数据
 *
 * @version V2.4
 * @date 2017/10/28
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class CCNStatisticModel extends StatisticModel
{
    /**
     * 本次分析前的遗留告警数
     */
    @JsonProperty("last_defect_count")
    private Integer lastDefectCount;

    /**
     * 本次分析的平均圈复杂度
     */
    @JsonProperty("average_ccn")
    private Float averageCCN;

    /**
     * 本次分析前的平均圈复杂度
     */
    @JsonProperty("last_average_ccn")
    private Float lastAverageCCN;

    /**
     * 本次分析的平均圈复杂度变化
     */
    @JsonProperty("average_ccn_change")
    private Float averageCCNChange;

    /**
     * 本次分析的极高级别风险函数数量
     */
    @JsonProperty("super_high_count")
    private Integer superHighCount;

    /**
     * 本次分析的高级别风险函数数量
     */
    @JsonProperty("high_count")
    private Integer highCount;

    /**
     * 本次分析的中级别风险函数数量
     */
    @JsonProperty("medium_count")
    private Integer mediumCount;

    /**
     * 本次分析的低级别风险函数数量
     */
    @JsonProperty("low_count")
    private Integer lowCount;

    /**
     * 风险函数超标圈复杂度总和
     */
    @JsonProperty("ccn_beyond_threshold_sum")
    private Integer ccnBeyondThresholdSum;

    @JsonProperty("high_fixed_count")
    private long highFixedCount;

    @JsonProperty("super_high_fixed_count")
    private long superHighFixedCount;

    @JsonProperty("middle_fixed_count")
    private long middleFixedCount;

    @JsonProperty("low_fixed_count")
    private long lowFixedCount;

    @JsonProperty("high_ignore_count")
    private long highIgnoreCount;

    @JsonProperty("super_high_ignore_count")
    private long superHighIgnoreCount;

    @JsonProperty("middle_ignore_count")
    private long middleIgnoreCount;

    @JsonProperty("low_ignore_count")
    private long lowIgnoreCount;

    @JsonProperty("high_mask_count")
    private long highMaskCount;

    @JsonProperty("super_high_mask_count")
    private long superHighMaskCount;

    @JsonProperty("middle_mask_count")
    private long middleMaskCount;

    @JsonProperty("low_mask_count")
    private long lowMaskCount;

    /**
     * 5日圈复杂度趋势图
     */
    @JsonProperty("average_list")
    private List<ChartAverageModel> averageList;

}
