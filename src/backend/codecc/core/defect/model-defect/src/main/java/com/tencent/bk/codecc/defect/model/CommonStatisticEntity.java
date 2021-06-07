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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 任务分析记录持久化对象
 *
 * @version V1.0
 * @date 2019/5/2
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_statistic")
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_tool_name_1", def = "{'task_id': 1, 'tool_name': 1}")
})
public class CommonStatisticEntity extends StatisticEntity
{
    /**
     * 新增告警数
     * 注：指真实新增，不考虑newDefectJudgeTime的逻辑过滤
     */
    @Field("new_count")
    private Integer newCount;

    @Field("exist_count")
    private Integer existCount;

    @Field("fixed_count")
    private Integer fixedCount;

    @Field("exclude_count")
    private Integer excludeCount;

    @Field("close_count")
    private Integer closeCount;

    @Field("exist_prompt_count")
    private Integer existPromptCount;

    @Field("exist_normal_count")
    private Integer existNormalCount;

    @Field("exist_serious_count")
    private Integer existSeriousCount;

    @Field("new_prompt_count")
    private Integer newPromptCount;

    @Field("new_normal_count")
    private Integer newNormalCount;

    @Field("new_serious_count")
    private Integer newSeriousCount;

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
    
    @Field("new_authors")
    private Set<String> newAuthors;

    /**
     * 仅指存量告警的处理人；原来的existAuthors指新+存量，2021.03.11
     */
    @Field("exist_authors")
    private Set<String> existAuthors;

    /**
     * "新"提示级别处理人(原指"新+存量”,2021.03.11)
     */
    @Field("prompt_authors")
    private Set<String> promptAuthors;

    /**
     * "新"一般级别处理人(原指"新+存量”,2021.03.11)
     */
    @Field("normal_authors")
    private Set<String> normalAuthors;

    /**
     * "新"严重级别处理人(原指"新+存量”,2021.03.11)
     */
    @Field("serious_authors")
    private Set<String> seriousAuthors;

    @Field("checker_statistic")
    private List<CheckerStatisticEntity> checkerStatistic;

    /**
     * 存量提示级别处理人
     */
    @Field("exist_prompt_authors")
    private Set<String> existPromptAuthors;

    /**
     * 存量一般级别处理人
     */
    @Field("exist_normal_authors")
    private Set<String> existNormalAuthors;

    /**
     * 存量严重级别处理人
     */
    @Field("exist_serious_authors")
    private Set<String> existSeriousAuthors;

    /**
     * 构造"零值"实例
     * 注意：由于common类提单CovDefectServiceImpl#commitDefect(..)是选择性upload/update，这会导致部分字段丢失，引发相关业务报空引用
     *
     * @return
     */
    public static CommonStatisticEntity constructByZeroVal() {
        return new CommonStatisticEntity(0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0L,
                0L, 0L, 0L, 0L,
                0L, 0L, 0L, 0L,
                new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(),
                new HashSet<>(), new ArrayList<>(), new HashSet<>(), new HashSet<>(),
                new HashSet<>());
    }
}
