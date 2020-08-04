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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Set;

/**
 * 任务分析记录持久化对象
 *
 * @version V1.0
 * @date 2019/5/2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_statistic")
public class CommonStatisticEntity extends StatisticEntity
{
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

    @Field("new_authors")
    private Set<String> newAuthors;

    @Field("exist_authors")
    private Set<String> existAuthors;
}
