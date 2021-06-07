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
 * 任务分析记录持久化对象
 *
 * @version V1.0
 * @date 2019/5/2
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LintStatisticModel extends StatisticModel
{
    @JsonProperty("file_count")
    private Integer fileCount;

    @JsonProperty("file_change")
    private Integer fileChange;

    @JsonProperty("new_defect_count")
    private Integer newDefectCount;

    @JsonProperty("history_defect_count")
    private Integer historyDefectCount;

    @JsonProperty("total_new_serious")
    private Integer totalNewSerious;

    @JsonProperty("total_new_normal")
    private Integer totalNewNormal;

    @JsonProperty("total_new_prompt")
    private Integer totalNewPrompt;

    @JsonProperty("total_serious")
    private Integer totalSerious;

    @JsonProperty("total_normal")
    private Integer totalNormal;

    @JsonProperty("total_prompt")
    private Integer totalPrompt;

    @JsonProperty("total_defect_count")
    private Long totalDefectCount;

    @JsonProperty("normal_fixed_count")
    private long normalFixedCount;

    @JsonProperty("prompt_fixed_count")
    private long promptFixedCount;

    @JsonProperty("serious_fixed_count")
    private long seriousFixedCount;

    @JsonProperty("normal_ignore_count")
    private long normalIgnoreCount;

    @JsonProperty("prompt_ignore_count")
    private long promptIgnoreCount;

    @JsonProperty("serious_ignore_count")
    private long seriousIgnoreCount;

    @JsonProperty("normal_mask_count")
    private long normalMaskCount;

    @JsonProperty("prompt_mask_count")
    private long promptMaskCount;

    @JsonProperty("serious_mask_count")
    private long seriousMaskCount;

    @JsonProperty("author_statistic")
    private List<NotRepairedAuthorModel> authorStatistic;

    @JsonProperty("checker_statistic")
    private List<CheckerStatisticModel> checkerStatistic;
}
