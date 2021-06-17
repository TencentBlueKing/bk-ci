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
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 忽略不扫描的规则模型
 *
 * @version V1.0
 * @date 2020/10/09
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IgnoreCheckerModel extends CommonModel {
    /**
     * 任务ID
     */
    @JsonProperty("task_id")
    private Long taskId;

    /**
     * 工具名称
     */
    @JsonProperty("tool_name")
    private String toolName;

    /**
     * 关闭的默认规则列表
     */
    @JsonProperty("close_default_checkers")
    private List<String> closeDefaultCheckers;

    /**
     * 打开的非默认规则列表
     */
    @JsonProperty("open_non_default_checkers")
    private List<String> openNonDefaultCheckers;
}
