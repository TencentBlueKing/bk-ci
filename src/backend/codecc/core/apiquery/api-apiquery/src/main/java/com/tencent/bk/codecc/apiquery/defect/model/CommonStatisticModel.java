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
import java.util.Set;
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
public class CommonStatisticModel extends StatisticModel
{
    @JsonProperty("new_count")
    private Integer newCount;

    @JsonProperty("exist_count")
    private Integer existCount;

    @JsonProperty("fixed_count")
    private Integer fixedCount;

    @JsonProperty("exclude_count")
    private Integer excludeCount;

    @JsonProperty("close_count")
    private Integer closeCount;

    @JsonProperty("exist_prompt_count")
    private Integer existPromptCount;

    @JsonProperty("exist_normal_count")
    private Integer existNormalCount;

    @JsonProperty("exist_serious_count")
    private Integer existSeriousCount;

    @JsonProperty("new_prompt_count")
    private Integer newPromptCount;

    @JsonProperty("new_normal_count")
    private Integer newNormalCount;

    @JsonProperty("new_serious_count")
    private Integer newSeriousCount;

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

    @JsonProperty("new_authors")
    private Set<String> newAuthors;

    @JsonProperty("exist_authors")
    private Set<String> existAuthors;

    @JsonProperty("checker_statistic")
    private List<CheckerStatisticModel> checkerStatistic;
}
