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

package com.tencent.bk.codecc.apiquery.defect.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 规则告警统计模型
 */
@Data
public class CheckerDefectStatModel {
    /**
     * 规则名称
     */
    @JsonProperty("checker_name")
    private String checkerName;

    /**
     * 工具
     */
    @JsonProperty("tool_name")
    private String toolName;

    /**
     * 规则创建时间
     */
    @JsonProperty("checker_create_date")
    private Long checkerCreatedDate;

    /**
     * 开启该规则的任务数
     */
    @JsonProperty("open_checker_task_count")
    private Integer openCheckerTaskCount;

    /**
     * 累计发现问题数
     */
    @JsonProperty("defect_total_count")
    private Integer defectTotalCount;

    /**
     * 待修复问题数
     */
    @JsonProperty("exist_count")
    private Integer existCount;

    /**
     * 已修复问题数
     */
    @JsonProperty("fixed_count")
    private Integer fixedCount;

    /**
     * 已忽略问题数
     */
    @JsonProperty("ignore_count")
    private Integer ignoreCount;

    /**
     * 已屏蔽问题数
     */
    @JsonProperty("excluded_count")
    private Integer excludedCount;

    /**
     * 统计日期
     */
    @JsonProperty("stat_date")
    private Long statDate;
}
