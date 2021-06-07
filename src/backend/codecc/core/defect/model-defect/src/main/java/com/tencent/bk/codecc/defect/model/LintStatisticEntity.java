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

package com.tencent.bk.codecc.defect.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 任务分析记录持久化对象
 *
 * @version V1.0
 * @date 2019/5/2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_lint_statistic")
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_tool_name_1", def = "{'task_id': 1, 'tool_name': 1}")
})
public class LintStatisticEntity extends StatisticEntity
{
    @Field("file_count")
    private Integer fileCount;

    @Field("file_change")
    private Integer fileChange;

    @Field("new_defect_count")
    private Integer newDefectCount;

    @Field("history_defect_count")
    private Integer historyDefectCount;

    @Field("total_new_serious")
    private Integer totalNewSerious;

    @Field("total_new_normal")
    private Integer totalNewNormal;

    @Field("total_new_prompt")
    private Integer totalNewPrompt;

    @Field("total_serious")
    private Integer totalSerious;

    @Field("total_normal")
    private Integer totalNormal;

    @Field("total_prompt")
    private Integer totalPrompt;

    @Field("total_defect_count")
    private Long totalDefectCount;

    @Field("normal_fixed_count")
    private long normalFixedCount;

    @Field("prompt_fixed_count")
    private long promptFixedCount;

    @Field("serious_fixed_count")
    private long seriousFixedCount;

    @Field("normal_ignore_count")
    private long normalIgnoreCount;

    @Field("prompt_ignore_count")
    private long promptIgnoreCount;

    @Field("serious_ignore_count")
    private long seriousIgnoreCount;

    @Field("normal_mask_count")
    private long normalMaskCount;

    @Field("prompt_mask_count")
    private long promptMaskCount;

    @Field("serious_mask_count")
    private long seriousMaskCount;

    @Field("author_statistic")
    private List<NotRepairedAuthorEntity> authorStatistic;

    @Field("checker_statistic")
    private List<CheckerStatisticEntity> checkerStatistic;

    /**
     * 存量告警处理人统计
     */
    @Field("exist_author_statistic")
    private List<NotRepairedAuthorEntity> existAuthorStatistic;
}
