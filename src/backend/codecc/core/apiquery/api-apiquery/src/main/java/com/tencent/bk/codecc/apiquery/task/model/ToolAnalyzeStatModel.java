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

import java.util.List;

/**
 * 工具执行统计实体类
 * @version V1.0
 * @date 2021/1/7
 */
@Data
public class ToolAnalyzeStatModel {
    /**
     * 日期
     */
    @JsonProperty("date")
    private String date;

    /**
     * 来源 user、gongfeng_scan
     */
    @JsonProperty("data_from")
    private String dataFrom;

    /**
     * 工具的名称
     */
    @JsonProperty("tool_name")
    private String toolName;

    /**
     * 工具执行状态 0：成功 1：失败
     */
    @JsonProperty("status")
    private Integer status;

    /**
     * 任务id列表
     */
    @JsonProperty("task_id_list")
    private List<Long> taskIdList;
}
