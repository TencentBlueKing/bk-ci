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

package com.tencent.bk.codecc.task.model;

import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 工具统计实体类
 *
 * @version V1.0
 * @date 2020/12/10
 */

@Data
@Document(collection = "t_tool_statistic")
public class ToolStatisticEntity {

    /**
     * 统计日期
     */
    @Field
    @Indexed
    private String date;

    /**
     * 统计数据来源: 开源/非开源(enum DefectStatType)
     */
    @Field("data_from")
    @Indexed(background = true)
    private String dataFrom;

    /**
     * 工具名
     */
    @Field("tool_name")
    @Indexed(background = true)
    private String toolName;

    /**
     * 工具总数
     */
    @Field("tool_count")
    private int toolCount;

    /**
     * 活跃工具数
     */
    @Field("active_count")
    private int activeCount;

    /**
     * 分析次数
     */
    @Field("analyze_count")
    private int analyzeCount;

}
