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
 * 规则参数实体类
 *
 * @version V2.0
 * @date 2020/5/13
 */
@Data
public class CheckerPropsModel
{
    /**
     * 规则集所在的工具名称
     */
    @JsonProperty("tool_name")
    private String toolName;

    /**
     * 工具特殊参数
     */
    @JsonProperty("param_json")
    private String paramJson;

    /**
     * 规则集所在的规则唯一键
     */
    @JsonProperty("checker_key")
    private String checkerKey;

    /**
     * 规则集所在的规则名称
     */
    @JsonProperty("checker_name")
    private String checkerName;

    /**
     * 规则集所在的工具名称
     */
    private String props;
}
