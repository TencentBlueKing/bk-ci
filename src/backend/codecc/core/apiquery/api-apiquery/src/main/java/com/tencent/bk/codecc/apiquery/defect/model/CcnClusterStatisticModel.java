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

import java.util.List;

/**
 * 圈复杂度维度统计model
 *
 * @version V1.0
 * @date 2021/3/23
 */

@Data
public class CcnClusterStatisticModel {

    /**
     * 任务ID
     */
    @JsonProperty("task_id")
    private Long taskId;

    /**
     * 构建ID
     */
    @JsonProperty("build_id")
    private String buildId;

    /**
     * 工具数量
     */
    @JsonProperty("tool_list")
    private List<String> toolList;

    /**
     * 统计的时间
     */
    @JsonProperty("time")
    private long time;

    @JsonProperty("defect_count")
    private Integer totalCount;

    @JsonProperty("new_defect_change")
    private Integer totalChange;

    @JsonProperty("average_thousand_defect")
    private Double averageThousandDefect;

    @JsonProperty("average_thousand_defect_change")
    private Double averageThousandDefectChange;

}
