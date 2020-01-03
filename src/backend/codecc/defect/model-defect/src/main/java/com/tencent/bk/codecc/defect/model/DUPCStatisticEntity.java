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

package com.tencent.bk.codecc.defect.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 每次分析结束的统计数据
 *
 * @version V2.4
 * @date 2017/10/28
 */

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_dupc_statistic")
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_tool_name_1", def = "{'task_id': 1, 'tool_name': 1}")
})
public class DUPCStatisticEntity extends StatisticEntity
{
    /**
     * 本次分析前的遗留告警数
     */
    @Field("last_defect_count")
    private int lastDefectCount;

    /**
     * 本次分析的代码重复率
     */
    @Field("dup_rate")
    private float dupRate;

    /**
     * 本次分析前的代码重复率
     */
    @Field("last_dup_rate")
    private float lastDupRate;

    /**
     * 本次分析的代码重复率变化值
     */
    @Field("dup_rate_change")
    private float dupRateChange;

    /**
     * 代码重复率工具的扫描统计结果
     */
    @Field("dupc_scan_summary")
    private DUPCScanSummaryEntity dupcScanSummary;
}
