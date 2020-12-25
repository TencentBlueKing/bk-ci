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
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 每次分析结束的统计数据
 *
 * @version V2.4
 * @date 2017/10/28
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class DUPCStatisticModel extends StatisticModel
{
    /**
     * 本次分析前的遗留告警数
     */
    @JsonProperty("last_defect_count")
    private Integer lastDefectCount;

    /**
     * 本次分析的代码重复率
     */
    @JsonProperty("dup_rate")
    private Float dupRate;

    /**
     * 本次分析前的代码重复率
     */
    @JsonProperty("last_dup_rate")
    private Float lastDupRate;

    /**
     * 本次分析的代码重复率变化值
     */
    @JsonProperty("dup_rate_change")
    private Float dupRateChange;

    /**
     * 代码重复率工具的扫描统计结果
     */
    @JsonProperty("dupc_scan_summary")
    private DUPCScanSummaryModel dupcScanSummary;

    /**
     * 极高风险文件数量
     */
    @JsonProperty("super_high_count")
    private Integer superHighCount;

    /**
     * 高风险文件数量
     */
    @JsonProperty("high_count")
    private Integer highCount;

    /**
     * 中风险文件数量
     */
    @JsonProperty("medium_count")
    private Integer mediumCount;

    /**
     * 平均圈复杂度趋势
     */
    @JsonProperty("dupc_chart")
    private List<DupcChartTrendModel> dupcChart;
}
