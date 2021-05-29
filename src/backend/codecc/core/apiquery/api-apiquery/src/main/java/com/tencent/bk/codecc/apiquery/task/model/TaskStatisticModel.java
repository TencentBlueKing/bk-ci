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

package com.tencent.bk.codecc.apiquery.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * TaskStatistic entity
 *
 * @version V1.0
 * @date 2019/12/11
 */
@Data
public class TaskStatisticModel {
    /**
     * 日期
     */
    @JsonProperty("date")
    private String date;

    /**
     * 数据来源: 开源/非开源(enum DefectStatType)
     */
    @JsonProperty("data_from")
    private String dataFrom;

    /**
     * 任务总数
     */
    @JsonProperty("task_count")
    private int taskCount;

    /**
     * 活跃任务数
     */
    @JsonProperty("active_count")
    private int activeCount;

    /**
     * 任务分析数
     */
    @JsonProperty("analyze_count")
    private int analyzeCount;
}
