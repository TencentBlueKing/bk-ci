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
 * 工具注册信息实体类
 *
 * @version V1.0
 * @date 2019/11/23
 */
@Data
public class ToolsRegisterStatisticsModel {
    /**
     * 配置信息对应的项目ID
     */
    @JsonProperty("task_id")
    private long taskId;

    /**
     * 工具的名称
     */
    @JsonProperty("tool_name")
    private String toolName;

    /**
     * 创建时间
     */
    @JsonProperty("create_date")
    private Long createDate;

    /**
     * 跟进状态
     */
    @JsonProperty("follow_status")
    private Integer followStatus;

    /**
     * 使用次数
     */
    @JsonProperty("register_count")
    private Integer registerCount;

}
