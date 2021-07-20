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
 * 统计工具分析平均耗时
 *
 * @version V1.0
 * @date 2021/1/12
 */

@Data
@Document(collection = "t_tool_elapse_time")
public class ToolElapseTimeEntity {

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
     * 超快增量、非超快增量
     */
    @Field("tool_name")
    @Indexed(background = true)
    private String toolName;

    /**
     * 超快增量、非超快增量
     */
    @Field("scan_stat_type")
    @Indexed(background = true)
    private String scanStatType;

    /**
     * 成功分析的总耗时
     */
    @Field("total_elapse_time")
    private long totalElapseTime;

    /**
     * 分析成功次数
     */
    @Field("succ_analyze_count")
    private long succAnalyzeCount;

    /**
     * 分析失败次数
     */
    @Field("fail_analyze_count")
    private long failAnalyzeCount;

}
