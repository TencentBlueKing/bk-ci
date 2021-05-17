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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.bk.codecc.apiquery.utils.EntityIdDeserializer;
import lombok.Data;

import java.util.List;

/**
 * 任务执行扫描工具记录的实体类
 *
 * @version V1.0
 * @date 2021/1/4
 */

@Data
public class TaskLogOverviewModel {

    @JsonProperty("_id")
    @JsonDeserialize(using = EntityIdDeserializer.class)
    private String entityId;

    @JsonProperty("task_id")
    private Long taskId;

    @JsonProperty("build_id")
    private String buildId;

    @JsonProperty("build_num")
    private String buildNum;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("start_time")
    private Long startTime;

    @JsonProperty("end_time")
    private Long endTime;

    @JsonProperty("task_log_list")
    private List<TaskLogModel> taskLogEntityList;

    @JsonProperty("tool_list")
    private List<String> toolList;

    /**
     * 统计分析次数
     */
    @JsonProperty("build_count")
    private Integer buildCount;
}
